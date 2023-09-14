package com.rarible.protocol.nft.listener.service.subscribers.royalties

import com.rarible.protocol.nft.core.model.RoyaltiesEvent
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.royalty.RoyaltiesSetForContractLogDescriptor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class RoyaltiesSetForContractLogSubscriber(descriptor: RoyaltiesSetForContractLogDescriptor) :
    AbstractItemLogEventSubscriber<RoyaltiesEvent>(SubscriberGroups.ROYALTIES_HISTORY, descriptor)
