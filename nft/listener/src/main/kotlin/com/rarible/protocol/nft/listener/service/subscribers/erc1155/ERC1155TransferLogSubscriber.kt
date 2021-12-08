package com.rarible.protocol.nft.listener.service.subscribers.erc1155

import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.ERC1155TransferLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class ERC1155TransferLogSubscriber(descriptor: ERC1155TransferLogDescriptor)
    : AbstractItemLogEventSubscriber<ItemTransfer>(descriptor)
