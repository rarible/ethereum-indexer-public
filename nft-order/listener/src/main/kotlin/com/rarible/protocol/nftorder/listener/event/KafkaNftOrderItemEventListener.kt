package com.rarible.protocol.nftorder.listener.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.dto.NftOrderItemEventTopicProvider
import com.rarible.protocol.nftorder.core.converter.ItemEventToDtoConverter
import com.rarible.protocol.nftorder.core.event.ItemEvent
import com.rarible.protocol.nftorder.core.event.ItemEventListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KafkaNftOrderItemEventListener(
    private val eventsProducer: RaribleKafkaProducer<NftOrderItemEventDto>
) : ItemEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val eventsHeaders = mapOf(
        "protocol.nft-order.event.version" to NftOrderItemEventTopicProvider.VERSION
    )


    override suspend fun onEvent(event: ItemEvent) {
        val dto = ItemEventToDtoConverter.convert(event)
        val message = KafkaMessage(
            id = dto.eventId,
            key = dto.itemId,
            value = dto,
            headers = eventsHeaders
        )
        eventsProducer.send(message).ensureSuccess()
        logger.info("Item Event sent: {}", dto)
    }
}
