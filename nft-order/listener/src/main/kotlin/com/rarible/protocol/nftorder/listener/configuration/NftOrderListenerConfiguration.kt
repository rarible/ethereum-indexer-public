package com.rarible.protocol.nftorder.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.nftorder.listener.handler.*
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.autoconfigure.OrderIndexerEventsSubscriberProperties
import com.rarible.protocol.unlockable.api.subscriber.UnlockableEventsConsumerFactory
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@EnableMongock
@EnableRaribleTask
@EnableScaletherMongoConversions
@EnableConfigurationProperties(
    value = [
        NftOrderListenerProperties::class,
        NftOrderJobProperties::class
    ]
)
class NftOrderListenerConfiguration(
    private val environmentInfo: ApplicationEnvironmentInfo,
    private val listenerProperties: NftOrderListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val blockchain: Blockchain,
    private val orderIndexerSubscriberProperties: OrderIndexerEventsSubscriberProperties
) {
    private val itemConsumerGroup = "${environmentInfo.name}.protocol.${blockchain.value}.nft-order.item"
    private val ownershipConsumerGroup = "${environmentInfo.name}.protocol.${blockchain.value}.nft-order.ownership"
    private val unlockableConsumerGroup = "${environmentInfo.name}.protocol.${blockchain.value}.nft-order.unlockable"
    private val orderConsumerGroup = "${environmentInfo.name}.protocol.${blockchain.value}.nft-order.order"
    private val orderPriceUpdateConsumerGroup = "${environmentInfo.name}.protocol.${blockchain.value}.nft-order.order-price-update"

    private val logger = LoggerFactory.getLogger(NftOrderListenerConfiguration::class.java)

    @Bean
    fun itemChangeWorker(
        nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
        itemEventHandler: ItemEventHandler
    ): ConsumerWorker<NftItemEventDto> {
        return ConsumerWorker(
            consumer = nftIndexerEventsConsumerFactory.createItemEventsConsumer(
                itemConsumerGroup,
                blockchain
            ),
            properties = listenerProperties.monitoringWorker,
            eventHandler = itemEventHandler,
            meterRegistry = meterRegistry,
            workerName = "itemEventDto"
        )
    }

    @Bean
    fun ownershipChangeWorker(
        nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
        ownershipEventHandler: OwnershipEventHandler
    ): BatchedConsumerWorker<NftOwnershipEventDto> {
        logger.info(
            "Creating batch of Ownership event consumers, number of consumers: {}",
            listenerProperties.ownershipConsumerCount
        )
        val consumers = (1..listenerProperties.ownershipConsumerCount).map {
            ConsumerWorker(
                consumer = nftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(
                    ownershipConsumerGroup,
                    blockchain
                ),
                properties = listenerProperties.monitoringWorker,
                eventHandler = ownershipEventHandler,
                meterRegistry = meterRegistry,
                workerName = "ownershipEventDto.$it"
            )
        }
        return BatchedConsumerWorker(consumers)
    }

    @Bean
    fun unlockableChangeWorker(
        unlockableEventsConsumerFactory: UnlockableEventsConsumerFactory,
        unlockableEventHandler: UnlockableEventHandler
    ): ConsumerWorker<UnlockableEventDto> {
        return ConsumerWorker(
            consumer = unlockableEventsConsumerFactory.createUnlockableEventsConsumer(
                unlockableConsumerGroup,
                blockchain
            ),
            properties = listenerProperties.monitoringWorker,
            eventHandler = unlockableEventHandler,
            meterRegistry = meterRegistry,
            workerName = "unlockableEventDto"
        )
    }

    @Bean
    fun orderChangeWorker(
        orderIndexerEventsConsumerFactory: OrderIndexerEventsConsumerFactory,
        orderEventHandler: OrderEventHandler
    ): BatchedConsumerWorker<OrderEventDto> {
        logger.info(
            "Creating batch of Order event consumers, number of consumers: {}",
            listenerProperties.orderConsumerCount
        )
        val consumers = (1..listenerProperties.orderConsumerCount).map {
            ConsumerWorker(
                consumer = createOrderEventsConsumer(orderConsumerGroup),
                properties = listenerProperties.monitoringWorker,
                eventHandler = orderEventHandler,
                meterRegistry = meterRegistry,
                workerName = "orderEventDto.$it"
            )
        }
        return BatchedConsumerWorker(consumers)
    }

    @Bean
    fun orderPriceChangeWorker(
        orderIndexerEventsConsumerFactory: OrderIndexerEventsConsumerFactory,
        orderPriceChangeEventHandler: OrderPriceChangeEventHandler
    ): BatchedConsumerWorker<NftOrdersPriceUpdateEventDto> {
        logger.info(
            "Creating batch of Order Price Update event consumers, number of consumers: {}",
            listenerProperties.orderConsumerCount
        )
        val consumers = (1..listenerProperties.orderConsumerCount).map {
            ConsumerWorker(
                consumer = orderIndexerEventsConsumerFactory.createNftOrdersPriceUpdateEventsConsumer(
                    orderPriceUpdateConsumerGroup,
                    blockchain
                ),
                properties = listenerProperties.monitoringWorker,
                eventHandler = orderPriceChangeEventHandler,
                meterRegistry = meterRegistry,
                workerName = "orderPriceUpdateEventDto.$it"
            )
        }
        return BatchedConsumerWorker(consumers)
    }

    // TODO remove when order-subscriber start to use .global topic by default
    fun createOrderEventsConsumer(consumerGroup: String): RaribleKafkaConsumer<OrderEventDto> {
        val clientIdPrefix = "${environmentInfo.name}.${blockchain.value}.${environmentInfo.name}.${UUID.randomUUID()}"
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.order-indexer-order-events-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = OrderEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = OrderIndexerTopicProvider.getOrderUpdateTopic(environmentInfo.name, blockchain.value) + ".global",
            bootstrapServers = orderIndexerSubscriberProperties.brokerReplicaSet
        )
    }
}
