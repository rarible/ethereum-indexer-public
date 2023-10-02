package com.rarible.protocol.nft.listener.service.subscribers.erc1155

import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.CreateERC1155RaribleLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import com.rarible.protocol.nft.listener.service.subscribers.AutoReduceService
import org.springframework.stereotype.Component

@Component
class CreateERC1155RaribleLogSubscriber(
    descriptor: CreateERC1155RaribleLogDescriptor,
    autoReduceService: AutoReduceService
) :
    AbstractItemLogEventSubscriber<CreateCollection>(SubscriberGroups.TOKEN_HISTORY, descriptor, autoReduceService)
