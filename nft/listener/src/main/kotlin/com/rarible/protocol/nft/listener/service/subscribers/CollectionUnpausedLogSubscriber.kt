package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.protocol.nft.core.model.CollectionPaused
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.CollectionUnpausedLogDescriptor
import org.springframework.stereotype.Component

@Component
class CollectionUnpausedLogSubscriber(
    descriptor: CollectionUnpausedLogDescriptor,
    autoReduceService: AutoReduceService
) : AbstractItemLogEventSubscriber<CollectionPaused>(SubscriberGroups.TOKEN_HISTORY, descriptor, autoReduceService)
