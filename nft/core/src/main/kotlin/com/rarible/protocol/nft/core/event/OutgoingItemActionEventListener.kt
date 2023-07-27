package com.rarible.protocol.nft.core.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.model.ActionEvent
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.KAFKA)
class OutgoingItemActionEventListener(
    private val eventPublisher: ProtocolNftEventPublisher
) : OutgoingEventListener<ActionEvent> {

    override suspend fun onEvent(event: ActionEvent) {
        eventPublisher.publish(event)
    }
}
