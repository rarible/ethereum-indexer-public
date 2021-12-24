package com.rarible.protocol.nft.listener.service.subscribers.creators

import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.creators.CreatorsLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class CreatorsLogSubscriber(descriptor: CreatorsLogDescriptor)
    : AbstractItemLogEventSubscriber<ItemCreators>(SubscriberGroups.ITEM_HISTORY, descriptor)
