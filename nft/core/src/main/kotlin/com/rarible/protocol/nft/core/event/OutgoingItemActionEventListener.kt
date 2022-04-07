package com.rarible.protocol.nft.core.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.KAFKA)
class OutgoingItemActionEventListener(
    private val eventPublisher: ProtocolNftEventPublisher
) : OutgoingEventListener<Action> {

    override suspend fun onEvent(event: Action) {
        eventPublisher.publish(event)
    }
}