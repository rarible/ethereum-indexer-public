package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.service.EntityEventListener
import org.springframework.stereotype.Component

@Component
class ItemEventReduceService(
    entityService: ItemUpdateService,
    entityEventService: ItemEventService,
    templateProvider: ItemTemplateProvider,
    reducer: CompositeItemReducer
) : EntityEventListener {
    private val delegate = EventReduceService(entityService, entityEventService, templateProvider, reducer)

    override val groupId: SubscriberGroup = SubscriberGroups.ITEM_HISTORY

    override suspend fun onEntityEvents(events: List<ReversedEthereumLogRecord>) {
        events
            .mapNotNull { ItemEventConverter.convert(it) }
            .let { delegate.reduceAll(it) }
    }
}
