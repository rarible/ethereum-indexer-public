package com.rarible.protocol.nft.listener.service.subscribers.erc1155

import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.ERC1155TransferBatchLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class ERC1155TransferBatchLogSubscriber(descriptor: ERC1155TransferBatchLogDescriptor)
    : AbstractItemLogEventSubscriber<ItemTransfer>(SubscriberGroups.ITEM_HISTORY, descriptor)
