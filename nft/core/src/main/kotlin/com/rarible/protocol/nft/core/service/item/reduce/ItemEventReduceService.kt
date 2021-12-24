package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.model.EntityEventListeners
import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.service.EntityEventListener
import org.springframework.stereotype.Component

@Component
class ItemEventReduceService(
    entityService: ItemUpdateService,
    entityIdService: ItemIdService,
    templateProvider: ItemTemplateProvider,
    reducer: ItemReducer,
    private val onNftItemLogEventListener: OnNftItemLogEventListener
) : EntityEventListener {
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    override val id: String = EntityEventListeners.ITEM_HISTORY_LISTENER

    override val groupId: SubscriberGroup = SubscriberGroups.ITEM_HISTORY

    override suspend fun onEntityEvents(events: List<LogRecordEvent<ReversedEthereumLogRecord>>) {
        events
            .onEach { onNftItemLogEventListener.onLogEvent(it) }
            .mapNotNull { ItemEventConverter.convert(it.record) }
            .let { delegate.reduceAll(it) }
    }
}
