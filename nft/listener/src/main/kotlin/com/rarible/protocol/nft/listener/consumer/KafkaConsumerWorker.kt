package com.rarible.protocol.nft.listener.consumer

interface KafkaConsumerWorker<T> : AutoCloseable {
    fun start()
}