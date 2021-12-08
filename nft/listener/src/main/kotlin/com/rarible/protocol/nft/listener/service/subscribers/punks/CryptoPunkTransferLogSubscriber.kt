package com.rarible.protocol.nft.listener.service.subscribers.punks

import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.listener.service.descriptors.crypto.punks.CryptoPunkTransferLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class CryptoPunkTransferLogSubscriber(descriptor: CryptoPunkTransferLogDescriptor)
    : AbstractItemLogEventSubscriber<ItemTransfer>(descriptor)
