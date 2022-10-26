package com.rarible.protocol.nft.core.service

import com.rarible.blockchain.scanner.ethereum.reduce.CompositeEntityEventListener
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.EntityEventListeners
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.service.item.reduce.ItemEventReduceService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipEventReduceService
import org.springframework.stereotype.Component

@Component
class EthereumCompositeEntityEventListener(
    itemEventReduceService: ItemEventReduceService,
    ownershipEventReduceService: OwnershipEventReduceService,
    properties: NftIndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo
) : CompositeEntityEventListener(
    entityEventsSubscribers = listOf(itemEventReduceService, ownershipEventReduceService),
    id = EntityEventListeners.itemHistoryListenerId(environmentInfo.name, properties.blockchain),
    subscriberGroup = SubscriberGroups.ITEM_HISTORY
)