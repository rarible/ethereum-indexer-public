package com.rarible.protocol.order.core.producer

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.AmmNftAssetTypeDto
import com.rarible.protocol.dto.AmmOrderNftUpdateEventDto
import com.rarible.protocol.dto.AssetTypeDto
import com.rarible.protocol.dto.CollectionAssetTypeDto
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc1155LazyAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.Erc721LazyAssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.GenerativeArtAssetTypeDto
import com.rarible.protocol.dto.NftOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.OrderActivityBidDto
import com.rarible.protocol.dto.OrderActivityCancelBidDto
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityListDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderIndexerTopicProvider
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties.PublishProperties
import com.rarible.protocol.order.core.misc.platform
import com.rarible.protocol.order.core.model.ItemId
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.KAFKA)
class ProtocolOrderPublisher(
    private val orderActivityProducer: RaribleKafkaProducer<ActivityDto>,
    private val orderEventProducer: RaribleKafkaProducer<OrderEventDto>,
    private val ordersPriceUpdateEventProducer: RaribleKafkaProducer<NftOrdersPriceUpdateEventDto>,
    private val publishProperties: PublishProperties
) {
    private val orderActivityHeaders = mapOf("protocol.order.activity.version" to ActivityTopicProvider.VERSION)
    private val orderEventHeaders = mapOf("protocol.order.event.version" to OrderIndexerTopicProvider.VERSION)

    suspend fun publish(event: OrderEventDto) {
        val (key, needPublish) = when (event) {
            is OrderUpdateEventDto ->
                (event.order.key ?: event.orderId) to event.order.platform.needPublish
            is AmmOrderNftUpdateEventDto ->
                event.orderId to publishProperties.publishAmmOrders
        }
        val message = KafkaMessage(
            key = key,
            value = event,
            headers = orderEventHeaders,
            id = event.eventId
        )
        if (needPublish) {
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

    suspend fun publish(event: NftOrdersPriceUpdateEventDto) {
        val message = KafkaMessage(
            key = ItemId(event.contract, event.tokenId).toString(),
            value = event,
            headers = orderEventHeaders,
            id = event.eventId
        )
        ordersPriceUpdateEventProducer.send(message).ensureSuccess()
    }

    private val OrderDto.key: String?
        get() = make.assetType.itemId ?: take.assetType.itemId

    private val PlatformDto.needPublish: Boolean
        get() = when (this) {
            PlatformDto.RARIBLE -> true
            PlatformDto.OPEN_SEA, -> publishProperties.publishSeaportOrders
            PlatformDto.X2Y2 -> publishProperties.publishX2Y2Orders
            PlatformDto.LOOKSRARE -> publishProperties.publishLooksrareOrders
            PlatformDto.CRYPTO_PUNKS -> publishProperties.publishCryptoPunksOrders
            PlatformDto.SUDOSWAP -> publishProperties.publishAmmOrders
            PlatformDto.BLUR -> publishProperties.publishBlurOrders
        }

    private val AssetTypeDto.itemId: String?
        get() = when (this) {
            is Erc721AssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc1155AssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc1155LazyAssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc721LazyAssetTypeDto -> ItemId(contract, tokenId).toString()
            is CryptoPunksAssetTypeDto -> ItemId(contract, tokenId.toBigInteger()).toString()
            is EthAssetTypeDto, is Erc20AssetTypeDto, is GenerativeArtAssetTypeDto, is CollectionAssetTypeDto, is AmmNftAssetTypeDto -> null
        }
}
