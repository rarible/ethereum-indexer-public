package com.rarible.protocol.nft.listener.service.subscribers.punks

import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.listener.service.descriptors.crypto.punks.CryptoPunkAssignLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class CryptoPunkAssignLogSubscriber(descriptor: CryptoPunkAssignLogDescriptor)
    : AbstractItemLogEventSubscriber<ItemTransfer>(descriptor)
