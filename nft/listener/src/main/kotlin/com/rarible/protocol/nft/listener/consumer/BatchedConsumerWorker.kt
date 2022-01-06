package com.rarible.protocol.nft.listener.consumer

import com.rarible.core.daemon.sequential.ConsumerBatchWorker

class BatchedConsumerWorker<T>(
    private val workers: List<ConsumerBatchWorker<T>>
) : KafkaConsumerWorker<T> {

    override fun start() {
        workers.forEach { it.start() }
    }

    override fun close() {
        workers.forEach { it.close() }
    }
}