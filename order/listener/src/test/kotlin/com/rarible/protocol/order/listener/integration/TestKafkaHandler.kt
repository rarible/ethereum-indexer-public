package com.rarible.protocol.order.listener.integration

import com.rarible.core.kafka.RaribleKafkaEventHandler
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue

class TestKafkaHandler<T> : RaribleKafkaEventHandler<T> {

    private val logger = LoggerFactory.getLogger(javaClass)

    val events = LinkedBlockingQueue<T>()

    override suspend fun handle(event: T) {
        logger.info("Test event received: {}", event)
        events.add(event)
    }

    fun clear() {
        events.clear()
    }
}
