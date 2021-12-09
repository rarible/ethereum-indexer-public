package com.rarible.protocol.nft.listener.service.subscribers.erc1155

import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.CreateERC1155LogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class CreateERC1155LogSubscriber(descriptor: CreateERC1155LogDescriptor)
    : AbstractItemLogEventSubscriber<CreateCollection>(SubscriberGroups.TOKEN_HISTORY, descriptor)
