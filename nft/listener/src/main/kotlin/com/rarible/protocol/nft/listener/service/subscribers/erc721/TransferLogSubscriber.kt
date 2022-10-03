package com.rarible.protocol.nft.listener.service.subscribers.erc721

import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc721.ERC721TransferLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class TransferLogSubscriber(descriptor: ERC721TransferLogDescriptor) :
    AbstractItemLogEventSubscriber<ItemTransfer>(SubscriberGroups.ITEM_HISTORY, descriptor) {
}
