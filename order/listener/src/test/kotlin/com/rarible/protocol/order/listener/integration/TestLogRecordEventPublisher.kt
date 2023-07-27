package com.rarible.protocol.order.listener.integration

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventListener
import com.rarible.blockchain.scanner.publisher.LogRecordEventPublisher
import org.springframework.stereotype.Component

// Replaces Kafka LogEvenRecord processing, used by BlockchainScanner by default
@Component
class TestLogRecordEventPublisher(
    listeners: List<LogRecordEventListener>
) : LogRecordEventPublisher {

    private val listeners = listeners.associateBy { it.groupId }

    override suspend fun publish(groupId: String, logRecordEvents: List<LogRecordEvent>) {
        return listeners[groupId]!!.onLogRecordEvents(logRecordEvents)
    }
}
