package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventsSubscriber
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withSpan
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.converters.model.OwnershipEventConverter
import com.rarible.protocol.nft.core.misc.addIndexerIn
import com.rarible.protocol.nft.core.misc.asEthereumLogRecord
import com.rarible.protocol.nft.core.model.ItemId
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
    private val itemEventConverter: ItemEventConverter,
    properties: NftIndexerProperties,
) : EntityEventsSubscriber {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val skipTransferContractTokens =
        properties.scannerProperties.skipTransferContractTokens.map(ItemIdFromStringConverter::convert)
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    suspend fun reduce(events: List<OwnershipEvent>) {
        delegate.reduceAll(events)
    }

    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        withSpan(
            name = "onOwnershipEvents",
            labels = listOf(
                "ownershipId" to (events.firstOrNull()
                    ?.let { itemEventConverter.convertToOwnershipId(it.record.asEthereumLogRecord()) }?.stringValue
                    ?: "")
            )
        ) {
            events
                .flatMap { eventConverter.convert(it.record.asEthereumLogRecord(), it.eventTimeMarks.addIndexerIn()) }
                .filter { event ->
                    OwnershipId.parseId(event.entityId)
                        .let { ItemId(it.token, it.tokenId) } !in skipTransferContractTokens
                }
                .let { delegate.reduceAll(it) }
        }
    }
}
