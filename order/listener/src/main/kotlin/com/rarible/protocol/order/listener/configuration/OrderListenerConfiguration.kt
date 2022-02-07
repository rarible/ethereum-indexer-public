package com.rarible.protocol.order.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
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
import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.job.OpenSeaOrdersFetcherWorker
import com.rarible.protocol.order.listener.service.event.Erc20BalanceConsumerEventHandler
import com.rarible.protocol.order.listener.service.event.NftOwnershipConsumerEventHandler
import com.rarible.protocol.order.listener.service.opensea.ExternalUserAgentProvider
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderService
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

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
    private val blockRepository: BlockRepository
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
    fun ownershipChangeWorker(orderBalanceService: OrderBalanceService): ConsumerWorker<NftOwnershipEventDto> {
        val consumer = nftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(
            ownershipBalanceConsumerGroup,
            blockchain = blockchain()
        )
        return ConsumerWorker(
            consumer = consumer,
            eventHandler = NftOwnershipConsumerEventHandler(orderBalanceService),
            meterRegistry = meterRegistry,
            workerName = "ownership-handler"
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
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER,
        name=["load-open-sea-orders"],
        havingValue="true",
        matchIfMissing = true
    )
    fun openSeaOrderLoadWorker(
        openSeaOrderService: OpenSeaOrderService,
        openSeaFetchStateRepository: OpenSeaFetchStateRepository,
        openSeaOrderConverter: OpenSeaOrderConverter,
        orderRepository: OrderRepository,
        orderUpdateService: OrderUpdateService,
        orderVersionListener: OrderVersionListener,
        meterRegistry: MeterRegistry,
        properties: OrderListenerProperties
    ): OpenSeaOrdersFetcherWorker {
        return OpenSeaOrdersFetcherWorker(
            properties = properties,
            openSeaOrderService = openSeaOrderService,
            openSeaFetchStateRepository = openSeaFetchStateRepository,
            openSeaOrderConverter = openSeaOrderConverter,
            orderRepository = orderRepository,
            orderUpdateService = orderUpdateService,
            meterRegistry = meterRegistry,
            workerProperties = DaemonWorkerProperties(pollingPeriod = Duration.ofSeconds(2), errorDelay = Duration.ofSeconds(2))
        ).apply { start() }
    }
}
