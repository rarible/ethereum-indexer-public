package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.misc.addIndexerIn
import com.rarible.protocol.nft.core.misc.asEthereumLogRecord
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemEventReduceService(
    entityService: ItemUpdateService,
    entityIdService: ItemIdService,
    templateProvider: ItemTemplateProvider,
    reducer: ItemReducer,
    private val onNftItemLogEventListener: OnNftItemLogEventListener,
    private val itemEventConverter: ItemEventConverter,
    properties: NftIndexerProperties,
) : LogRecordEventSubscriber {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val skipTransferContractTokens =
        properties.scannerProperties.skipTransferContractTokens.map(ItemIdFromStringConverter::convert)
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    suspend fun reduce(events: List<ItemEvent>) {
        delegate.reduceAll(events)
    }

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        val start = nowMillis()
        val markedEvents = events.map { it.copy(eventTimeMarks = it.eventTimeMarks.addIndexerIn(start)) }
        try {
            onNftItemLogEventListener.onLogEvents(markedEvents)
            markedEvents
                .mapNotNull { itemEventConverter.convert(it.record.asEthereumLogRecord(), it.eventTimeMarks) }
                .filter { ItemId.parseId(it.entityId) !in skipTransferContractTokens }
                .let { delegate.reduceAll(it) }
        } catch (ex: Exception) {
            logger.error("Error on entity events $events", ex)
            throw ex
        }
        logger.info(
            "Handled {} Item blockchain events ({}ms)",
            events.size,
            System.currentTimeMillis() - start.toEpochMilli()
        )
    }
}
