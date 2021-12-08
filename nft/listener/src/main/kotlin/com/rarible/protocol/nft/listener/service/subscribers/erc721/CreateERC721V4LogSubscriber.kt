package com.rarible.protocol.nft.listener.service.subscribers.erc721

import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.listener.service.descriptors.erc721.CreateERC721V4LogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class CreateERC721V4LogSubscriber(descriptor: CreateERC721V4LogDescriptor)
    : AbstractItemLogEventSubscriber<CreateCollection>(descriptor)
