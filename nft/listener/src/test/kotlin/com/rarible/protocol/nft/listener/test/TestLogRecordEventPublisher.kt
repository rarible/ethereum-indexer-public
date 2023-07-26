package com.rarible.protocol.nft.listener.test

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.publisher.LogRecordEventPublisher
import org.springframework.stereotype.Component

// Replaces Kafka LogEvenRecord processing, used by BlockchainScanner by default
@Component
class TestLogRecordEventPublisher(
    listeners: List<EntityEventListener>
) : LogRecordEventPublisher {

    private val listeners = listeners.associateBy { it.subscriberGroup }

    override suspend fun publish(groupId: String, logRecordEvents: List<LogRecordEvent>) {
        return listeners[groupId]!!.onEntityEvents(logRecordEvents)
    }
}
