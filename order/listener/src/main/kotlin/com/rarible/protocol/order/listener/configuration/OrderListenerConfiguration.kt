package com.rarible.protocol.order.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.task.EnableRaribleTask
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.erc20.api.subscriber.Erc20IndexerEventsConsumerFactory
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.job.LooksrareOrdersFetchWorker
import com.rarible.protocol.order.listener.job.OrderStartEndCheckerWorker
import com.rarible.protocol.order.listener.job.RaribleBidsCanceledAfterExpiredJob
import com.rarible.protocol.order.listener.job.ReservoirOrdersReconciliationWorker
import com.rarible.protocol.order.listener.job.SeaportOrdersFetchWorker
import com.rarible.protocol.order.listener.job.X2Y2CancelEventsFetchWorker
import com.rarible.protocol.order.listener.job.X2Y2OrdersFetchWorker
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.service.event.Erc20BalanceConsumerEventHandler
import com.rarible.protocol.order.listener.service.event.ItemConsumerEventHandler
import com.rarible.protocol.order.listener.service.event.OwnershipConsumerEventHandler
import com.rarible.protocol.order.listener.service.looksrare.LooksrareOrderLoadHandler
import com.rarible.protocol.order.listener.service.opensea.ExternalUserAgentProvider
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderService
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderValidator
import com.rarible.protocol.order.listener.service.opensea.SeaportOrderLoadHandler
import com.rarible.protocol.order.listener.service.opensea.SeaportOrderLoader
import com.rarible.protocol.order.listener.service.order.OrderStartEndCheckerHandler
import com.rarible.protocol.order.listener.service.order.SeaportOrdersLoadTaskHandler
import com.rarible.protocol.order.listener.service.x2y2.X2Y2CancelEventsLoadHandler
import com.rarible.protocol.order.listener.service.x2y2.X2Y2CancelListEventLoader
import com.rarible.protocol.order.listener.service.x2y2.X2Y2OrderLoadHandler
import com.rarible.protocol.order.listener.service.x2y2.X2Y2OrderLoader
import com.rarible.reservoir.client.ReservoirClient
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
@EnableRaribleTask
@EnableConfigurationProperties(OrderIndexerProperties::class, OrderListenerProperties::class)
class OrderListenerConfiguration(
    private val environmentInfo: ApplicationEnvironmentInfo,
    private val commonProperties: OrderIndexerProperties,
    private val listenerProperties: OrderListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val erc20IndexerEventsConsumerFactory: Erc20IndexerEventsConsumerFactory,
    private val nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
    private val metrics: ForeignOrderMetrics
) {
    private val erc20BalanceConsumerGroup =
        "protocol.${commonProperties.blockchain.value}.order.indexer.erc20-balance"
    private val ownershipBalanceConsumerGroup =
        "protocol.${commonProperties.blockchain.value}.order.indexer.ownership"
    private val itemConsumerGroup =
        "protocol.${commonProperties.blockchain.value}.order.indexer.item"

    @Bean
    fun blockchain(): Blockchain {
        return commonProperties.blockchain
    }

    @Bean
    fun raribleKafkaConsumerFactory(): RaribleKafkaConsumerFactory {
        return RaribleKafkaConsumerFactory(
            env = environmentInfo.name,
            host = environmentInfo.host
        )
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
    fun x2y2OrderLoadProperties(): X2Y2OrderLoadProperties {
        return listenerProperties.x2y2Load
    }

    @Bean
    fun x2y2CancelListEventLoadProperties(): X2Y2EventLoadProperties {
        return listenerProperties.x2y2CancelListEventLoad
    }

    @Bean
    fun sudoSwapLoadProperties(): SudoSwapLoadProperties {
        return listenerProperties.sudoSwapLoad
    }

    @Bean
    fun startEndWorkerProperties(): StartEndWorkerProperties {
        return listenerProperties.startEndWorker
    }

    @Bean
    fun floorBidCheckWorkerProperties(): FloorOrderCheckWorkerProperties {
        return listenerProperties.floorOrderCheckWorker
    }

    @Bean
    fun reservoirProperties(): ReservoirProperties {
        return listenerProperties.reservoir
    }

    @Bean
    fun erc20BalanceChangeWorker(
        factory: RaribleKafkaConsumerFactory,
        handler: Erc20BalanceConsumerEventHandler
    ): RaribleKafkaConsumerWorker<Erc20BalanceEventDto> {
        val settings = erc20IndexerEventsConsumerFactory.createErc20BalanceEventsKafkaConsumerSettings(
            group = erc20BalanceConsumerGroup,
            blockchain = blockchain(),
            concurrency = listenerProperties.balanceConsumerWorkersCount,
            batchSize = listenerProperties.balanceConsumerBatchSize,
        )
        return factory.createWorker(
            settings = settings,
            handler = handler
        )
    }

    @Bean
    fun ownershipChangeWorker(
        factory: RaribleKafkaConsumerFactory,
        handler: OwnershipConsumerEventHandler
    ): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        val settings = nftIndexerEventsConsumerFactory.createOwnershipEventsKafkaConsumerSettings(
            ownershipBalanceConsumerGroup,
            blockchain = blockchain(),
            concurrency = listenerProperties.ownershipConsumerWorkersCount,
            batchSize = listenerProperties.ownershipConsumerBatchSize,
        )
        return factory.createWorker(
            settings = settings,
            handler = handler
        )
    }

    @Bean
    fun itemChangeWorker(
        factory: RaribleKafkaConsumerFactory,
        handler: ItemConsumerEventHandler
    ): RaribleKafkaConsumerWorker<NftItemEventDto> {
        val settings = nftIndexerEventsConsumerFactory.createItemEventsKafkaConsumerSettings(
            consumerGroup = itemConsumerGroup,
            blockchain = blockchain(),
            concurrency = listenerProperties.itemConsumerWorkersCount,
            batchSize = listenerProperties.itemConsumerBatchSize,
        )
        return factory.createWorker(
            settings = settings,
            handler = handler
        )
    }

    @Bean
    @ExperimentalCoroutinesApi
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER,
        name = ["start-end-worker.enabled"],
        havingValue = "true"
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
        name = ["rarible-expired-bid-worker.enabled"],
        havingValue = "true"
    )
    fun raribleBidsCanceledAfterExpiredJob(
        orderRepository: OrderRepository,
        orderVersionRepository: OrderVersionRepository,
        orderUpdateService: OrderUpdateService,
        raribleOrderExpiration: OrderIndexerProperties.RaribleOrderExpirationProperties,
        properties: OrderListenerProperties,
        meterRegistry: MeterRegistry
    ): RaribleBidsCanceledAfterExpiredJob {
        return RaribleBidsCanceledAfterExpiredJob(
            orderRepository,
            orderVersionRepository,
            orderUpdateService,
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
        properties: SeaportLoadProperties
    ): SeaportOrdersFetchWorker {
        val loader = SeaportOrderLoader(
            openSeaOrderService = openSeaOrderService,
            openSeaOrderConverter = openSeaOrderConverter,
            openSeaOrderValidator = openSeaOrderValidator,
            orderRepository = orderRepository,
            orderUpdateService = orderUpdateService,
            properties = properties,
            metrics = metrics
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
        properties: SeaportLoadProperties
    ): SeaportOrdersLoadTaskHandler {
        val loader = SeaportOrderLoader(
            openSeaOrderService = openSeaOrderService,
            openSeaOrderConverter = openSeaOrderConverter,
            openSeaOrderValidator = openSeaOrderValidator,
            orderRepository = orderRepository,
            orderUpdateService = orderUpdateService,
            properties = properties,
            metrics = metrics
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
        properties: X2Y2OrderLoadProperties
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
        name = ["x2y2-cancel-list-event-load.enabled"],
        havingValue = "true"
    )
    fun x2y2CancelListFetchWorker(
        meterRegistry: MeterRegistry,
        stateRepository: AggregatorStateRepository,
        x2y2CancelListEventLoader: X2Y2CancelListEventLoader,
        properties: X2Y2EventLoadProperties
    ): X2Y2CancelEventsFetchWorker {
        val handler = X2Y2CancelEventsLoadHandler(
            stateRepository,
            x2y2CancelListEventLoader,
            properties
        )
        return X2Y2CancelEventsFetchWorker(
            handler = handler,
            properties = listenerProperties.x2y2CancelListEventLoad,
            meterRegistry = meterRegistry
        ).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER,
        name = ["looksrare-load.enabled"],
        havingValue = "true"
    )
    fun looksrareOrderLoadWorker(
        meterRegistry: MeterRegistry,
        properties: LooksrareLoadProperties,
        looksrareOrderLoadHandler: LooksrareOrderLoadHandler
    ): LooksrareOrdersFetchWorker {
        return LooksrareOrdersFetchWorker(
            properties = properties,
            meterRegistry = meterRegistry,
            handler = looksrareOrderLoadHandler
        ).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER,
        name = ["reservoir.enabled"],
        havingValue = "true"
    )
    fun reservoirOrdersReconciliationWorker(
        meterRegistry: MeterRegistry,
        stateRepository: AggregatorStateRepository,
        properties: ReservoirProperties,
        reservoirClient: ReservoirClient,
        orderCancelService: OrderCancelService,
        orderRepository: OrderRepository,
        foreignOrderMetrics: ForeignOrderMetrics,
    ): ReservoirOrdersReconciliationWorker {
        return ReservoirOrdersReconciliationWorker(
            reservoirClient = reservoirClient,
            aggregatorStateRepository = stateRepository,
            orderCancelService = orderCancelService,
            properties = properties,
            orderRepository = orderRepository,
            foreignOrderMetrics = foreignOrderMetrics,
            meterRegistry = meterRegistry,
        ).apply { start() }
    }
}
