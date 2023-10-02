package com.rarible.protocol.nft.listener.service.subscribers.erc721

import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc721.CollectionOwnershipTransferLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import com.rarible.protocol.nft.listener.service.subscribers.AutoReduceService
import org.springframework.stereotype.Component

@Component
class CollectionOwnershipTransferLogSubscriber(
    descriptor: CollectionOwnershipTransferLogDescriptor,
    autoReduceService: AutoReduceService
) : AbstractItemLogEventSubscriber<CollectionOwnershipTransferred>(
    SubscriberGroups.TOKEN_HISTORY,
    descriptor,
    autoReduceService
)
