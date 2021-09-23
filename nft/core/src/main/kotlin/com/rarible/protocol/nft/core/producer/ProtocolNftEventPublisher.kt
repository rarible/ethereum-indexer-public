package com.rarible.protocol.nft.core.producer

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.core.model.OwnershipId

class ProtocolNftEventPublisher(
    private val collectionEventProducer: RaribleKafkaProducer<NftCollectionEventDto>,
    private val itemEventProducer: RaribleKafkaProducer<NftItemEventDto>,
    private val internalItemEventProducer: RaribleKafkaProducer<NftItemEventDto>,
    private val ownershipEventProducer: RaribleKafkaProducer<NftOwnershipEventDto>,
    private val collectionActivityProducer: RaribleKafkaProducer<ActivityDto>,
    private val nftItemActivityProducer: RaribleKafkaProducer<ActivityDto>
) {
    private val collectionEventHeaders = mapOf("protocol.collection.event.version" to NftCollectionEventTopicProvider.VERSION)
    private val itemEventHeaders = mapOf("protocol.item.event.version" to NftItemEventTopicProvider.VERSION)
    private val ownershipEventHeaders =
        mapOf("protocol.ownership.event.version" to NftOwnershipEventTopicProvider.VERSION)
    private val collectionActivityHeaders = mapOf("protocol.collection.activity.version" to ActivityTopicProvider.VERSION)
    private val itemActivityHeaders = mapOf("protocol.item.activity.version" to ActivityTopicProvider.VERSION)

    suspend fun publish(event: NftCollectionEventDto) {
        val message = KafkaMessage(
            key = event.id.hex(),
            value = event,
            headers = collectionEventHeaders,
            id = event.eventId
        )
        collectionEventProducer.send(message).ensureSuccess()
    }

    suspend fun publish(event: NftItemEventDto) {
        val message = KafkaMessage(
            key = event.itemId,
            value = event,
            headers = itemEventHeaders,
            id = event.eventId
        )
        itemEventProducer.send(message).ensureSuccess()
    }

    suspend fun publishInternalItem(event: NftItemEventDto) {
        val message = KafkaMessage(
            key = event.itemId,
            value = event,
            headers = itemEventHeaders,
            id = event.eventId
        )
        internalItemEventProducer.send(message).ensureSuccess()
    }

    suspend fun publish(event: NftOwnershipEventDto) {
        val ownershipId = OwnershipId.parseId(event.ownershipId)
        val itemId = "${ownershipId.token}:${ownershipId.tokenId.value}"

        val message = KafkaMessage(
            key = itemId,
            value = event,
            headers = ownershipEventHeaders,
            id = event.eventId
        )
        ownershipEventProducer.send(message).ensureSuccess()
    }

    suspend fun publish(event: NftCollectionActivityDto) {
        val message = KafkaMessage(
            key = event.contract.hex(),
            value = event as ActivityDto,
            headers = collectionActivityHeaders,
            id = event.id
        )
        collectionActivityProducer.send(message).ensureSuccess()
    }

    suspend fun publish(event: NftActivityDto) {
        val itemId = "${event.contract}:${event.tokenId}"
        val message = KafkaMessage(
            key = itemId,
            value = event as ActivityDto,
            headers = itemActivityHeaders,
            id = itemId
        )
        nftItemActivityProducer.send(message).ensureSuccess()
    }
}
