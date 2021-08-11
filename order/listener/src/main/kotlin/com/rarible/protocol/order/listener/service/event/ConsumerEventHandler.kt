package com.rarible.protocol.order.listener.service.event

interface ConsumerEventHandler<T> {
    suspend fun handle(event: T)
}