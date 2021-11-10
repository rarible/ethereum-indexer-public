package com.rarible.protocol.order.core.producer

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties.PublishProperties
import com.rarible.protocol.order.core.model.ItemId

class ProtocolAuctionPublisher(
    private val auctionActivityProducer: RaribleKafkaProducer<ActivityDto>,
    private val auctionEventProducer: RaribleKafkaProducer<AuctionEventDto>,
    private val publishProperties: PublishProperties
) {
    private val auctionActivityHeaders = mapOf("protocol.auction.activity.version" to ActivityTopicProvider.VERSION)
    private val auctionEventHeaders = mapOf("protocol.auction.event.version" to OrderIndexerTopicProvider.VERSION)

    suspend fun publish(event: AuctionEventDto) {
        val key = when (event) {
            is AuctionUpdateEventDto -> event.auction.key ?: event.auctionId
            is AuctionDeleteEventDto -> event.auctionId
        }
        val message = KafkaMessage(
            key = key,
            value = event,
            headers = auctionEventHeaders,
            id = event.eventId
        )
        auctionEventProducer.send(message).ensureSuccess()
    }

    suspend fun publish(event: AuctionActivityDto) {
        val key = when (event) {
            is AuctionActivityOpenDto -> event.transactionHash.toString()
            is AuctionActivityBidDto -> event.transactionHash.toString()
            is AuctionActivityFinishDto -> event.transactionHash.toString()
            is AuctionActivityCancelDto -> event.transactionHash.toString()
        }
        val message = KafkaMessage(
            key = key,
            value = event as ActivityDto,
            headers = auctionActivityHeaders,
            id = event.id
        )
        if (publishProperties.publishAuctionActivity) {
            auctionActivityProducer.send(message).ensureSuccess()
        }
    }

    private val AuctionDto.key: String?
        get() = sell.assetType.itemId

    private val AssetTypeDto.itemId: String?
        get() = when (this) {
            is Erc721AssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc1155AssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc1155LazyAssetTypeDto -> ItemId(contract, tokenId).toString()
            is Erc721LazyAssetTypeDto -> ItemId(contract, tokenId).toString()
            is CryptoPunksAssetTypeDto -> ItemId(contract, punkId.toBigInteger()).toString()
            is EthAssetTypeDto, is Erc20AssetTypeDto, is GenerativeArtAssetTypeDto -> null
        }
}
