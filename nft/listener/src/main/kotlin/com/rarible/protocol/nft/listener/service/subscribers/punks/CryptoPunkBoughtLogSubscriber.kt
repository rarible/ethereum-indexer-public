package com.rarible.protocol.nft.listener.service.subscribers.punks

import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.crypto.punks.CryptoPunkBoughtLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import com.rarible.protocol.nft.listener.service.subscribers.AutoReduceService
import org.springframework.stereotype.Component

@Component
class CryptoPunkBoughtLogSubscriber(descriptor: CryptoPunkBoughtLogDescriptor, autoReduceService: AutoReduceService) :
    AbstractItemLogEventSubscriber<ItemTransfer>(SubscriberGroups.ITEM_HISTORY, descriptor, autoReduceService)
