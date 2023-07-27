package com.rarible.protocol.nft.core

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue

class TestKafkaHandler<T> : ConsumerEventHandler<T> {

    private val logger = LoggerFactory.getLogger(javaClass)

    val events = LinkedBlockingQueue<T>()

    override suspend fun handle(event: T) {
        logger.info("Test Item event received: {}", event)
        events.add(event)
    }

    fun clear() {
        logger.info("Test Item events cleaned up")
        events.clear()
    }
}
