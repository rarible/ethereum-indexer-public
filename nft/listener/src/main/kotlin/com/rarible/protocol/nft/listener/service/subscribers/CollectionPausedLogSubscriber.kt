package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.protocol.nft.core.model.CollectionPaused
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.CollectionPausedLogDescriptor
import org.springframework.stereotype.Component

@Component
class CollectionPausedLogSubscriber(descriptor: CollectionPausedLogDescriptor, autoReduceService: AutoReduceService) :
    AbstractItemLogEventSubscriber<CollectionPaused>(SubscriberGroups.TOKEN_HISTORY, descriptor, autoReduceService)
