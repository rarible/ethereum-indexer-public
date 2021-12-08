package com.rarible.protocol.nft.listener.service.subscribers.erc721

import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.listener.service.descriptors.erc721.CreateERC721RaribleUserLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class CreateERC721RaribleUserLogSubscriber(descriptor: CreateERC721RaribleUserLogDescriptor)
    : AbstractItemLogEventSubscriber<CreateCollection>(descriptor)
