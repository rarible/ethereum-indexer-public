package com.rarible.protocol.order.core.producer

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties.PublishProperties
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.Platform

class ProtocolOrderPublisher(
    private val orderActivityProducer: RaribleKafkaProducer<ActivityDto>,
    private val orderEventProducer: RaribleKafkaProducer<OrderEventDto>,
    private val globalOrderEventProducer: RaribleKafkaProducer<OrderEventDto>,
    private val publishProperties: PublishProperties
) {
    private val orderActivityHeaders = mapOf("protocol.order.activity.version" to OrderIndexerTopicProvider.VERSION)
    private val orderEventHeaders = mapOf("protocol.order.event.version" to OrderIndexerTopicProvider.VERSION)

    suspend fun publish(event: OrderEventDto) {
        val (key, platform) = when (event) {
            is OrderUpdateEventDto ->
                (event.order.key ?: event.orderId) to event.order.platform
        }
        val message = KafkaMessage(
            key = key,
            value = event as OrderEventDto,
            headers = orderEventHeaders,
            id = event.eventId
        )
        globalOrderEventProducer.send(message).ensureSuccess()

        if (platform.needPublish) {
            orderEventProducer.send(message).ensureSuccess()
        }
    }

    suspend fun publish(event: OrderActivityDto) {
        val key = when (event) {
            is OrderActivityMatchDto -> event.transactionHash.toString()
            is OrderActivityBidDto -> event.hash.toString()
            is OrderActivityListDto -> event.hash.toString()
            is OrderActivityCancelListDto -> event.hash.toString()
            is OrderActivityCancelBidDto -> event.hash.toString()
        }
        val message = KafkaMessage(
            key = key,
            value = event as ActivityDto,
            headers = orderActivityHeaders,
            id = event.id
        )
        orderActivityProducer.send(message).ensureSuccess()
    }

    private val OrderDto.key: String?
        get() = make.assetType.itemId ?: take.assetType.itemId

    private val OrderDto.platform: Platform
        get() = when (this) {
            is LegacyOrderDto, is RaribleV2OrderDto -> Platform.RARIBLE
            is OpenSeaV1OrderDto -> Platform.OPEN_SEA
        }

    private val AssetTypeDto.itemId: String?
        get() = when (this) {
            is Erc721AssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc1155AssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc1155LazyAssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc721LazyAssetTypeDto -> ItemId(contract, tokenId).toString()
            is CryptoPunksAssetTypeDto -> ItemId(contract, punkId.toBigInteger()).toString()
            is EthAssetTypeDto, is Erc20AssetTypeDto, is GenerativeArtAssetTypeDto -> null
            is FlowAssetTypeDto -> throw UnsupportedOperationException("Unsupported assert type ${this.javaClass}")
        }

    private val Platform.needPublish: Boolean
        get() = when (this) {
            Platform.RARIBLE -> true
            Platform.OPEN_SEA -> publishProperties.publishOpenSeaOrdersToCommonTopic
        }
}
