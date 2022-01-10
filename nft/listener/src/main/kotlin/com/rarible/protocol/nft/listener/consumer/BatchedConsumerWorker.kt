package com.rarible.protocol.nft.listener.consumer

import com.rarible.core.daemon.sequential.ConsumerWorker

class BatchedConsumerWorker<T>(
    private val workers: List<ConsumerWorker<T>>
) : KafkaConsumerWorker<T> {

    override fun start() {
        workers.forEach { it.start() }
    }

    override fun close() {
        workers.forEach { it.close() }
    }
}