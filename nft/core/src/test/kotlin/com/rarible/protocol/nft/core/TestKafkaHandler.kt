package com.rarible.protocol.nft.core

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import java.util.concurrent.LinkedBlockingQueue

class TestKafkaHandler<T> : ConsumerEventHandler<T> {

    val events = LinkedBlockingQueue<T>()

    override suspend fun handle(event: T) {
        events.add(event);
    }

}