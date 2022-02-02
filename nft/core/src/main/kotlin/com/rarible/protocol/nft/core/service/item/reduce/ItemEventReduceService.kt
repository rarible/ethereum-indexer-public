package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withTransaction
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.model.EntityEventListeners
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.EntityEventListener
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class ItemEventReduceService(
    entityService: ItemUpdateService,
    entityIdService: ItemIdService,
    templateProvider: ItemTemplateProvider,
    reducer: ItemReducer,
    private val onNftItemLogEventListener: OnNftItemLogEventListener,
    private val properties: NftIndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo
) : EntityEventListener {

    private val skipTransferContractTokens = properties.scannerProperties.skipTransferContractTokens.map(ItemIdFromStringConverter::convert)
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    override val id: String = EntityEventListeners.itemHistoryListenerId(environmentInfo.name, properties.blockchain)

    override val subscriberGroup: SubscriberGroup = SubscriberGroups.ITEM_HISTORY

    suspend fun reduce(events: List<ItemEvent>) {
        delegate.reduceAll(events)
    }

    override suspend fun onEntityEvents(events: List<LogRecordEvent<ReversedEthereumLogRecord>>) {
        withContext(ReduceContext(skipOwnerships = properties.reduceProperties.skipOwnerships)) {
            withTransaction(name = "onItemEvents", labels = listOf("size" to events.size)) {
                events
                    .onEach { onNftItemLogEventListener.onLogEvent(it) }
                    .mapNotNull { ItemEventConverter.convert(it.record) }
                    .filter { itemEvent -> ItemId.parseId(itemEvent.entityId) !in skipTransferContractTokens }
                    .let { delegate.reduceAll(it) }
            }
        }
    }
}
