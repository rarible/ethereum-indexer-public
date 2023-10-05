package com.rarible.protocol.nft.listener.service.subscribers.erc721

import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc721.CreateERC721RaribleUserLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import com.rarible.protocol.nft.listener.service.subscribers.AutoReduceService
import org.springframework.stereotype.Component

@Component
class CreateERC721RaribleUserLogSubscriber(
    descriptor: CreateERC721RaribleUserLogDescriptor,
    autoReduceService: AutoReduceService
) : AbstractItemLogEventSubscriber<CreateCollection>(SubscriberGroups.TOKEN_HISTORY, descriptor, autoReduceService)
