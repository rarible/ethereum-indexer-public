package com.rarible.protocol.order.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.ethereum.listener.log.persist.BlockRepository
import com.rarible.ethereum.monitoring.BlockchainMonitoringWorker
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.erc20.api.subscriber.Erc20IndexerEventsConsumerFactory
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.consumer.BatchedConsumerWorker
import com.rarible.protocol.order.listener.job.LooksrareOrdersFetchWorker
import com.rarible.protocol.order.listener.job.OrderStartEndCheckerWorker
import com.rarible.protocol.order.listener.job.RaribleBidsCanceledAfterExpiredJob
import com.rarible.protocol.order.listener.job.SeaportOrdersFetchWorker
import com.rarible.protocol.order.listener.job.X2Y2OrdersFetchWorker
import com.rarible.protocol.order.listener.service.event.Erc20BalanceConsumerEventHandler
import com.rarible.protocol.order.listener.service.event.NftOwnershipConsumerEventHandler
import com.rarible.protocol.order.listener.service.looksrare.LooksrareOrderLoadHandler
import com.rarible.protocol.order.listener.service.opensea.ExternalUserAgentProvider
import com.rarible.protocol.order.listener.service.opensea.MeasurableOpenSeaOrderService
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderService
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderValidator
import com.rarible.protocol.order.listener.service.opensea.SeaportOrderLoadHandler
import com.rarible.protocol.order.listener.service.opensea.SeaportOrderLoader
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import com.rarible.protocol.order.listener.service.order.OrderStartEndCheckerHandler
import com.rarible.protocol.order.listener.service.order.SeaportOrdersLoadTaskHandler
import com.rarible.protocol.order.listener.service.x2y2.X2Y2OrderLoadHandler
import com.rarible.protocol.order.listener.service.x2y2.X2Y2OrderLoader
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableMongock
@EnableContractService
@EnableScaletherMongoConversions
@EnableLogListeners(scanPackage = [OrderListenerConfiguration::class])
@EnableConfigurationProperties(OrderIndexerProperties::class, OrderListenerProperties::class)
class OrderListenerConfiguration(
    environmentInfo: ApplicationEnvironmentInfo,
    private val commonProperties: OrderIndexerProperties,
    private val listenerProperties: OrderListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val erc20IndexerEventsConsumerFactory: Erc20IndexerEventsConsumerFactory,
    private val nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val blockRepository: BlockRepository,
    private val micrometer: MeterRegistry,
    private val seaportLoadCounter: RegisteredCounter,
    private val seaportOrderDelayGauge : RegisteredGauge<Long>
) {
    private val erc20BalanceConsumerGroup = "${environmentInfo.name}.protocol.${commonProperties.blockchain.value}.order.indexer.erc20-balance"
    private val ownershipBalanceConsumerGroup = "${environmentInfo.name}.protocol.${commonProperties.blockchain.value}.order.indexer.ownership"

    @Bean
    fun featureFlags(): OrderIndexerProperties.FeatureFlags {
        return commonProperties.featureFlags
    }

    @Bean
    fun blockchain(): Blockchain {
        return commonProperties.blockchain
    }

    @Bean
    fun externalUserAgentProvider(): ExternalUserAgentProvider {
        return ExternalUserAgentProvider(listenerProperties.openSeaClientUserAgents)
    }

    @Bean
    fun seaportLoadProperties(): SeaportLoadProperties {
        return listenerProperties.seaportLoad
    }

    @Bean
    fun looksrareLoadProperties(): LooksrareLoadProperties {
        return listenerProperties.looksrareLoad
    }

    @Bean
    fun x2y2LoadProperties(): X2Y2LoadProperties {
        return listenerProperties.x2y2Load
    }

    @Bean
    fun startEndWorkerProperties(): StartEndWorkerProperties {
        return listenerProperties.startEndWorker
    }

    @Bean
    fun erc20BalanceChangeWorker(orderBalanceService: OrderBalanceService): ConsumerWorker<Erc20BalanceEventDto> {
        val args = erc20IndexerEventsConsumerFactory.createErc20BalanceEventsConsumer(
            consumerGroup = erc20BalanceConsumerGroup,
            blockchain = blockchain()
        )
        val consumer = RaribleKafkaConsumer<Erc20BalanceEventDto>(
            clientId = args.clientId,
            consumerGroup = args.consumerGroup,
            valueDeserializerClass = args.valueDeserializerClass,
            defaultTopic = args.defaultTopic,
            bootstrapServers = args.bootstrapServers,
            offsetResetStrategy = args.offsetResetStrategy
        )
        return ConsumerWorker(
            consumer = consumer,
            eventHandler = Erc20BalanceConsumerEventHandler(orderBalanceService),
            meterRegistry = meterRegistry,
            workerName = "erc20-balance-handler"
        ).apply { start() }
    }

    @Bean
    fun ownershipChangeWorker(orderBalanceService: OrderBalanceService): BatchedConsumerWorker<NftOwnershipEventDto> {
        val consumer = nftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(
            ownershipBalanceConsumerGroup,
            blockchain = blockchain()
        )
        return BatchedConsumerWorker(
            (1..listenerProperties.ownershipConsumerWorkersCount).map { index ->
                ConsumerWorker(
                    consumer = consumer,
                    eventHandler = NftOwnershipConsumerEventHandler(orderBalanceService),
                    meterRegistry = meterRegistry,
                    workerName = "ownership-handler-$index"
                )
            }
        ).apply { start() }
    }

    // TODO: this bean is apparently configured in the ethereum-core (BlockchainMonitoringConfiguration), no need to configure here.
    @Bean
    fun blockchainMonitoringWorker(): BlockchainMonitoringWorker {
        return BlockchainMonitoringWorker(
            properties = listenerProperties.monitoringWorker,
            blockchain = commonProperties.blockchain,
            meterRegistry = meterRegistry,
            blockRepository = blockRepository
        )
    }

    @Bean
    @ExperimentalCoroutinesApi
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER,
        name=["start-end-worker.enabled"],
        havingValue="true"
    )
    fun startEndWorker(
        handler: OrderStartEndCheckerHandler,
        properties: StartEndWorkerProperties,
        meterRegistry: MeterRegistry
    ): OrderStartEndCheckerWorker {
        return OrderStartEndCheckerWorker(handler, properties, meterRegistry).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER,
        name=["rarible-expired-bid-worker.enabled"],
        havingValue="true"
    )
    fun raribleBidsCanceledAfterExpiredJob(
        orderRepository: OrderRepository,
        orderReduceService: OrderReduceService,
        raribleOrderExpiration: OrderIndexerProperties.RaribleOrderExpirationProperties,
        properties: OrderListenerProperties,
        meterRegistry: MeterRegistry
    ): RaribleBidsCanceledAfterExpiredJob {
        return RaribleBidsCanceledAfterExpiredJob(
            orderRepository,
            orderReduceService,
            raribleOrderExpiration,
            properties,
            meterRegistry
        ).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.seaport-load.enabled"], havingValue = "true")
    fun seaportOrdersFetchWorker(
        openSeaOrderService: OpenSeaOrderService,
        openSeaFetchStateRepository: OpenSeaFetchStateRepository,
        aggregatorStateRepository: AggregatorStateRepository,
        openSeaOrderConverter: OpenSeaOrderConverter,
        openSeaOrderValidator: OpenSeaOrderValidator,
        orderRepository: OrderRepository,
        orderUpdateService: OrderUpdateService,
        properties: SeaportLoadProperties,
        seaportSaveCounter: RegisteredCounter,
    ): SeaportOrdersFetchWorker {
        val loader = SeaportOrderLoader(
            openSeaOrderService = measurableOpenSeaOrderService(openSeaOrderService),
            openSeaOrderConverter = openSeaOrderConverter,
            openSeaOrderValidator = openSeaOrderValidator,
            orderRepository = orderRepository,
            orderUpdateService = orderUpdateService,
            properties = properties,
            seaportSaveCounter = seaportSaveCounter
        )
        val handler = SeaportOrderLoadHandler(
            seaportOrderLoader = loader,
            openSeaFetchStateRepository = openSeaFetchStateRepository,
            aggregatorStateRepository = aggregatorStateRepository,
            properties = properties
        )
        return SeaportOrdersFetchWorker(
            handler = handler,
            properties = properties,
            meterRegistry = meterRegistry
        ).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.seaport-load.enabled"], havingValue = "true")
    fun seaportOrdersLoadTaskHandler(
        openSeaOrderService: OpenSeaOrderService,
        openSeaOrderConverter: OpenSeaOrderConverter,
        openSeaOrderValidator: OpenSeaOrderValidator,
        orderRepository: OrderRepository,
        orderUpdateService: OrderUpdateService,
        properties: SeaportLoadProperties,
        seaportTaskSaveCounter: RegisteredCounter,
        seaportTaskLoadCounter: RegisteredCounter,
    ): SeaportOrdersLoadTaskHandler {
        val loader = SeaportOrderLoader(
            openSeaOrderService = measurableOpenSeaOrderService(
                openSeaOrderService = openSeaOrderService,
                seaportCounter = seaportTaskLoadCounter,
                measureDelay = false,
            ),
            openSeaOrderConverter = openSeaOrderConverter,
            openSeaOrderValidator = openSeaOrderValidator,
            orderRepository = orderRepository,
            orderUpdateService = orderUpdateService,
            properties = properties,
            seaportSaveCounter = seaportTaskSaveCounter
        )
        return SeaportOrdersLoadTaskHandler(loader)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER,
        name = ["x2y2-load.enabled"],
        havingValue = "true"
    )
    fun x2y2OrderFetchWorker(
        meterRegistry: MeterRegistry,
        stateRepository: AggregatorStateRepository,
        x2y2OrderLoader: X2Y2OrderLoader,
        properties: X2Y2LoadProperties
    ): X2Y2OrdersFetchWorker {
        val handler = X2Y2OrderLoadHandler(
            stateRepository,
            x2y2OrderLoader,
            properties
        )
        return X2Y2OrdersFetchWorker(
            handler = handler,
            properties = listenerProperties.x2y2Load,
            meterRegistry = meterRegistry
        ).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER,
        name=["looksrare-load.enabled"],
        havingValue="true"
    )
    fun looksrareOrderLoadWorker(
        meterRegistry: MeterRegistry,
        properties: OrderListenerProperties,
        looksrareOrderLoadHandler: LooksrareOrderLoadHandler
    ): LooksrareOrdersFetchWorker {
        return LooksrareOrdersFetchWorker(
            properties = properties.looksrareLoad,
            meterRegistry = meterRegistry,
            handler = looksrareOrderLoadHandler
        ).apply { start() }
    }

    private fun measurableOpenSeaOrderService(
        openSeaOrderService: OpenSeaOrderService,
        measureDelay: Boolean = true,
        seaportCounter: RegisteredCounter = seaportLoadCounter
    ): MeasurableOpenSeaOrderService {
        return MeasurableOpenSeaOrderService(
            delegate = openSeaOrderService,
            micrometer = micrometer,
            blockchain = blockchain(),
            seaportLoadCounter = seaportCounter,
            seaportDelayGauge = seaportOrderDelayGauge,
            measureDelay = measureDelay
        )
    }
}
