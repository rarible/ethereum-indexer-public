package com.rarible.protocol.unlockable.api.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.UnlockableEventDto
import com.rarible.protocol.dto.UnlockableTopicProvider
import com.rarible.protocol.unlockable.converter.LockEventDtoConverter
import com.rarible.protocol.unlockable.event.LockEvent
import com.rarible.protocol.unlockable.event.LockEventListener
import org.springframework.stereotype.Component

@Component
class KafkaLockEventListener(
    private val converter: LockEventDtoConverter,
    private val eventsProducer: RaribleKafkaProducer<UnlockableEventDto>
) : LockEventListener {

    private val eventsHeaders = mapOf(
        "protocol.unlockable.event.version" to UnlockableTopicProvider.VERSION
    )

    override suspend fun onEvent(event: LockEvent) {
        val dto = converter.convert(event)
        val message = KafkaMessage(
            id = dto.eventId,
            key = dto.itemId,
            value = dto,
            headers = eventsHeaders
        )
        eventsProducer.send(message).ensureSuccess()
    }
}
