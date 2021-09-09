package com.rarible.protocol.nftorder.listener.configuration

import com.rarible.core.daemon.sequential.ConsumerWorker

class BatchedConsumerWorker<T>(
    private val consumers: List<ConsumerWorker<T>>
) : AutoCloseable {

    fun start() {
        consumers.forEach { it.start() }
    }

    override fun close() {
        consumers.forEach { it.close() }
    }

}