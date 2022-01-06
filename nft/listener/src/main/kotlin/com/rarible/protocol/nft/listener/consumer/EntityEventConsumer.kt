package com.rarible.protocol.nft.listener.consumer

import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.service.EntityEventListener

interface EntityEventConsumer : AutoCloseable {
    fun start(handler: Map<SubscriberGroup, EntityEventListener>)
}