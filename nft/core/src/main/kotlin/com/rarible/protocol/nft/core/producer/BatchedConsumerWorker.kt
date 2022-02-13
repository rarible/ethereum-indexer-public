package com.rarible.protocol.nft.core.producer

import com.rarible.core.daemon.sequential.AbstractConsumerWorker

class BatchedConsumerWorker<T>(
    private val consumers: List<AbstractConsumerWorker<T, *>>
) : AutoCloseable {

    fun start() {
        consumers.forEach { it.start() }
    }

    override fun close() {
        consumers.forEach { it.close() }
    }

}