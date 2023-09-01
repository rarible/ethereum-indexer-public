package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.converters.model.OwnershipEventConverter
import com.rarible.protocol.nft.core.misc.addIndexerIn
import com.rarible.protocol.nft.core.misc.asEthereumLogRecord
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipEventReduceService(
    entityService: OwnershipUpdateService,
    entityIdService: OwnershipIdService,
    templateProvider: OwnershipTemplateProvider,
    reducer: OwnershipReducer,
    private val eventConverter: OwnershipEventConverter,
    properties: NftIndexerProperties,
) : LogRecordEventSubscriber {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val skipTransferContractTokens =
        properties.scannerProperties.skipTransferContractTokens.map(ItemIdFromStringConverter::convert)
            .toHashSet()
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    suspend fun reduce(events: List<OwnershipEvent>) {
        delegate.reduceAll(events)
    }

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        val start = nowMillis()
        val ethRecords = events.map { it.record.asEthereumLogRecord() to it.eventTimeMarks.addIndexerIn(start) }
        eventConverter.convert(ethRecords)
            .filter { OwnershipId.parseId(it.entityId).toItemId() !in skipTransferContractTokens }
            .let { delegate.reduceAll(it) }

        logger.info(
            "Handled {} Ownership blockchain events ({}ms)",
            events.size,
            System.currentTimeMillis() - start.toEpochMilli()
        )
    }
}
