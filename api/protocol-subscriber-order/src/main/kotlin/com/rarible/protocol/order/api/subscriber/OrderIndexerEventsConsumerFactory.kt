package com.rarible.protocol.order.api.subscriber

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.AuctionEventDto
import com.rarible.protocol.dto.NftOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderIndexerTopicProvider
import java.util.*

class OrderIndexerEventsConsumerFactory(
    private val brokerReplicaSet: String,
    private val host: String,
    private val environment: String
) {
    fun createOrderEventsConsumer(consumerGroup: String, blockchain: Blockchain): RaribleKafkaConsumer<OrderEventDto> {
        return RaribleKafkaConsumer(
            clientId = "${createClientIdPrefix(blockchain)}.order-indexer-order-events-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = OrderEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = OrderIndexerTopicProvider.getOrderUpdateTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createNftOrdersPriceUpdateEventsConsumer(consumerGroup: String, blockchain: Blockchain): RaribleKafkaConsumer<NftOrdersPriceUpdateEventDto> {
        return RaribleKafkaConsumer(
            clientId = "${createClientIdPrefix(blockchain)}.order-indexer-nft-orders-price-update-events-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftOrdersPriceUpdateEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = OrderIndexerTopicProvider.getPriceUpdateTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createAuctionEventsConsumer(consumerGroup: String, blockchain: Blockchain): RaribleKafkaConsumer<AuctionEventDto> {
        return RaribleKafkaConsumer(
            clientId = "${createClientIdPrefix(blockchain)}.order-indexer-auction-events-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = AuctionEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = OrderIndexerTopicProvider.getAuctionUpdateTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    private fun createClientIdPrefix(blockchain: Blockchain): String {
        return "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"
    }
}
