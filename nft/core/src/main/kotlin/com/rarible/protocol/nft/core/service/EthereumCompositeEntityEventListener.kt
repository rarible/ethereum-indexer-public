package com.rarible.protocol.nft.core.service

import com.rarible.blockchain.scanner.framework.listener.AbstractLogRecordEventListener
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
    properties: NftIndexerProperties
) : AbstractLogRecordEventListener(
    id = EntityEventListeners.itemHistoryListenerId(properties.blockchain),
    groupId = SubscriberGroups.ITEM_HISTORY,
    subscribers = listOf(itemEventReduceService, ownershipEventReduceService),
)
