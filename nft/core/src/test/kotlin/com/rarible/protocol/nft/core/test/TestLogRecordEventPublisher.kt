package com.rarible.protocol.nft.core.test

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventListener
import com.rarible.blockchain.scanner.publisher.LogRecordEventPublisher
import org.springframework.stereotype.Component

@Component
class TestLogRecordEventPublisher(
    listeners: List<LogRecordEventListener>
) : LogRecordEventPublisher {

    private val listeners = listeners.associateBy { it.groupId }

    override suspend fun publish(groupId: String, logRecordEvents: List<LogRecordEvent>) {
        return listeners[groupId]!!.onLogRecordEvents(logRecordEvents)
    }
}
