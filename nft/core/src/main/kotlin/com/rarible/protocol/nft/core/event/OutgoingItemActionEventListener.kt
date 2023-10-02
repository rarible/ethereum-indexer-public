package com.rarible.protocol.nft.core.event

import com.rarible.protocol.nft.core.model.ActionEvent
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.stereotype.Component

@Component
class OutgoingItemActionEventListener(
    private val eventPublisher: ProtocolNftEventPublisher
) : OutgoingEventListener<ActionEvent> {

    override suspend fun onEvent(event: ActionEvent) {
        eventPublisher.publish(event)
    }
}
