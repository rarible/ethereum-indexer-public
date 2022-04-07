package com.rarible.protocol.nft.core.producer

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipId
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.KAFKA)
class ProtocolNftEventPublisher(
    private val collectionEventsProducer: RaribleKafkaProducer<NftCollectionEventDto>,
    private val internalCollectionEventsProducer: RaribleKafkaProducer<NftCollectionEventDto>,
    private val itemEventsProducer: RaribleKafkaProducer<NftItemEventDto>,
    private val ownershipEventProducer: RaribleKafkaProducer<NftOwnershipEventDto>,
    private val nftItemActivityProducer: RaribleKafkaProducer<ActivityDto>,
    private val actionProducer: RaribleKafkaProducer<Action>
) {
    suspend fun publish(event: NftCollectionEventDto) {
        val message = KafkaMessage(
            key = event.id.hex(),
            value = event,
            headers = COLLECTION_EVENT_HEADERS,
            id = event.eventId
        )
        collectionEventsProducer.send(message).ensureSuccess()
        logger.info("Sent collection event ${event.eventId}: $event")
    }

    suspend fun publishInternalCollection(event: NftCollectionEventDto) {
        val message = KafkaMessage(
            key = event.id.hex(),
            value = event,
            headers = COLLECTION_EVENT_HEADERS,
            id = event.eventId
        )
        internalCollectionEventsProducer.send(message).ensureSuccess()
        logger.info("Sent internal collection event ${event.eventId}: $event")
    }

    suspend fun publish(event: NftItemEventDto) {
        val message = KafkaMessage(
            key = event.itemId,
            value = event,
            headers = ITEM_EVENT_HEADERS,
            id = event.eventId
        )
        itemEventsProducer.send(message).ensureSuccess()
        logger.info("Sent item event ${event.itemId}: ${event.toShort()}")
    }

    suspend fun publish(event: NftOwnershipEventDto) {
        val message = prepareOwnershipKafkaMessage(event)
        ownershipEventProducer.send(message).ensureSuccess()
        logger.info("Sent ownership event ${event.eventId}: $event")
    }

    suspend fun publish(events: List<NftOwnershipEventDto>) {
        val messages = events.map { event -> prepareOwnershipKafkaMessage(event) }
        ownershipEventProducer.send(messages).collect { result ->
            result.ensureSuccess()
        }
        events.forEach {
            logger.info("Sent ownership event ${it.eventId}: $it")
        }
    }

    suspend fun publish(event: NftActivityDto) {
        val itemId = "${event.contract}:${event.tokenId}"
        val message = KafkaMessage(
            key = itemId,
            value = event as ActivityDto,
            headers = ITEM_ACTIVITY_HEADERS,
            id = itemId
        )
        nftItemActivityProducer.send(message).ensureSuccess()
        logger.info("Sent item activity event ${event.id}: $event")
    }

    suspend fun publish(event: Action) {
        val itemId = ItemId(event.token, event.tokenId)
        val message = KafkaMessage(
            key = itemId.stringValue,
            value = event,
            headers = ACTION_HEADERS,
            id = itemId.stringValue
        )
        actionProducer.send(message).ensureSuccess()
        logger.info("Sent action event for ${itemId.decimalStringValue}: $event")
    }

    private fun prepareOwnershipKafkaMessage(event: NftOwnershipEventDto): KafkaMessage<NftOwnershipEventDto> {
        val ownershipId = OwnershipId.parseId(event.ownershipId)
        val itemId = "${ownershipId.token}:${ownershipId.tokenId.value}"

        return KafkaMessage(
            key = itemId,
            value = event,
            headers = OWNERSHIP_EVENT_HEADERS,
            id = event.eventId
        )
    }

    private fun NftItemEventDto.toShort() =
        when (this) {
            is NftItemUpdateEventDto -> copy(item = item.copy(owners = emptyList()))
            is NftItemDeleteEventDto -> this
        }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProtocolNftEventPublisher::class.java)

        val COLLECTION_EVENT_HEADERS = mapOf("protocol.collection.event.version" to NftCollectionEventTopicProvider.VERSION)
        val ITEM_EVENT_HEADERS = mapOf("protocol.item.event.version" to NftItemEventTopicProvider.VERSION)
        val OWNERSHIP_EVENT_HEADERS = mapOf("protocol.ownership.event.version" to NftOwnershipEventTopicProvider.VERSION)
        val ITEM_ACTIVITY_HEADERS = mapOf("protocol.item.activity.version" to ActivityTopicProvider.VERSION)
        val ACTION_HEADERS = mapOf("protocol.item.internal.action.version" to InternalTopicProvider.VERSION)
    }
}
