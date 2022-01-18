package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.EntityEventListener
import org.springframework.stereotype.Component

@Component
class ItemEventReduceService(
    entityService: ItemUpdateService,
    entityIdService: ItemIdService,
    templateProvider: ItemTemplateProvider,
    reducer: ItemReducer,
    private val onNftItemLogEventListener: OnNftItemLogEventListener,
    properties: NftIndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo
) : EntityEventListener {
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    override val id: String = EntityEventListeners.itemHistoryListenerId(environmentInfo.name, properties.blockchain.name)

    override val groupId: SubscriberGroup = SubscriberGroups.ITEM_HISTORY

    suspend fun reduce(events: List<ItemEvent>) {
        delegate.reduceAll(events)
    }

    override suspend fun onEntityEvents(events: List<LogRecordEvent<ReversedEthereumLogRecord>>) {
        events
            .onEach { onNftItemLogEventListener.onLogEvent(it) }
            .mapNotNull { ItemEventConverter.convert(it.record) }
            .let { delegate.reduceAll(it) }
    }
}
