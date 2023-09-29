package com.rarible.protocol.nft.listener.service.subscribers.erc1155

import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.CreateERC1155RaribleUserLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import com.rarible.protocol.nft.listener.service.subscribers.AutoReduceService
import org.springframework.stereotype.Component

@Component
class CreateERC1155RaribleUserLogSubscriber(
    descriptor: CreateERC1155RaribleUserLogDescriptor,
    autoReduceService: AutoReduceService
) : AbstractItemLogEventSubscriber<CreateCollection>(SubscriberGroups.TOKEN_HISTORY, descriptor, autoReduceService)
