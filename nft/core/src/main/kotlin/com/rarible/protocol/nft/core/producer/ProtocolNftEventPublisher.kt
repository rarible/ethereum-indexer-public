package com.rarible.protocol.nft.core.producer

import com.rarible.core.common.EventTimeMarks
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.monitoring.EventCountMetrics
import com.rarible.ethereum.monitoring.EventCountMetrics.EventType
import com.rarible.ethereum.monitoring.EventCountMetrics.Stage
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.misc.addIndexerOut
import com.rarible.protocol.nft.core.misc.toDto
import com.rarible.protocol.nft.core.model.ActionEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ProtocolNftEventPublisher(
    private val collectionEventsProducer: RaribleKafkaProducer<NftCollectionEventDto>,
    private val itemEventsProducer: RaribleKafkaProducer<NftItemEventDto>,
    private val itemMetaEventsProducer: RaribleKafkaProducer<NftItemMetaEventDto>,
    private val ownershipEventProducer: RaribleKafkaProducer<NftOwnershipEventDto>,
    private val nftItemActivityProducer: RaribleKafkaProducer<EthActivityEventDto>,
    private val actionProducer: RaribleKafkaProducer<ActionEvent>,
    private val properties: NftIndexerProperties,
    private val eventCountMetrics: EventCountMetrics
) {
    suspend fun publish(event: NftCollectionEventDto) = withMetric(EventType.COLLECTION) {
        val message = KafkaMessage(
            key = event.id.hex(),
            value = event,
            headers = COLLECTION_EVENT_HEADERS,
            id = event.eventId
        )
        collectionEventsProducer.send(message).ensureSuccess()
        logger.info("Sent collection event ${event.eventId}: $event")
    }

    suspend fun publish(event: NftItemEventDto) = withMetric(EventType.ITEM) {
        val message = KafkaMessage(
            key = event.itemId,
            value = event,
            headers = ITEM_EVENT_HEADERS,
            id = event.eventId
        )
        itemEventsProducer.send(message).ensureSuccess()
        logger.info("Sent item event ${event.itemId}: ${event.toShort()}")
    }

    suspend fun publish(event: NftItemMetaEventDto) {
        val message = KafkaMessage(
            key = event.itemId,
            value = event,
            headers = ITEM_EVENT_HEADERS,
            id = event.eventId
        )
        itemMetaEventsProducer.send(message).ensureSuccess()
        logger.info("Sent item meta event ${event.itemId}: $event")
    }

    suspend fun publish(event: NftOwnershipEventDto) = withMetric(EventType.OWNERSHIP) {
        val message = prepareOwnershipKafkaMessage(event)
        ownershipEventProducer.send(message).ensureSuccess()
        logger.info("Sent ownership event ${event.eventId}: $event")
    }

    suspend fun publish(activities: List<Pair<NftActivityDto, EventTimeMarks>>) {
        val messages = activities.map {
            val activity = it.first
            val eventTimeMarks = it.second
            val itemId = "${activity.contract}:${activity.tokenId}"
            val message = KafkaMessage(
                key = itemId,
                value = EthActivityEventDto(activity, eventTimeMarks.addIndexerOut().toDto()),
                headers = ITEM_ACTIVITY_HEADERS,
                id = itemId
            )
            logger.info("Sent item activity event ${activity.id}: $activity")
            message
        }
        nftItemActivityProducer.send(messages).collect {
            withMetric(EventType.ACTIVITY) {
                it.ensureSuccess()
            }
        }
    }

    suspend fun publish(event: ActionEvent) = withMetric(EventType.AUCTION) {
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

    private suspend fun withMetric(type: EventType, delegate: suspend () -> Unit) {
        try {
            eventCountMetrics.eventSent(Stage.INDEXER, properties.blockchain.value, type)
            delegate()
        } catch (e: Exception) {
            eventCountMetrics.eventSent(Stage.INDEXER, properties.blockchain.value, type, -1)
            throw e
        }
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

        val COLLECTION_EVENT_HEADERS =
            mapOf("protocol.collection.event.version" to NftCollectionEventTopicProvider.VERSION)
        val ITEM_EVENT_HEADERS = mapOf("protocol.item.event.version" to NftItemEventTopicProvider.VERSION)
        val OWNERSHIP_EVENT_HEADERS =
            mapOf("protocol.ownership.event.version" to NftOwnershipEventTopicProvider.VERSION)
        val ITEM_ACTIVITY_HEADERS = mapOf("protocol.item.activity.version" to ActivityTopicProvider.VERSION)
        val ACTION_HEADERS = mapOf("protocol.item.internal.action.version" to InternalTopicProvider.VERSION)
    }
}
