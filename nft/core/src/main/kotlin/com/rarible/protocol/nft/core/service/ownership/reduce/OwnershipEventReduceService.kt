package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withSpan
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.converters.model.OwnershipEventConverter
import com.rarible.protocol.nft.core.model.*
import org.springframework.stereotype.Component

@Component
class OwnershipEventReduceService(
    entityService: OwnershipUpdateService,
    entityIdService: OwnershipIdService,
    templateProvider: OwnershipTemplateProvider,
    reducer: OwnershipReducer,
    private val eventConverter: OwnershipEventConverter,
    properties: NftIndexerProperties
) {

    private val skipTransferContractTokens = properties.scannerProperties.skipTransferContractTokens.map(ItemIdFromStringConverter::convert)
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    suspend fun reduce(events: List<OwnershipEvent>) {
        delegate.reduceAll(events)
    }

    suspend fun onEntityEvents(events: List<LogRecordEvent<ReversedEthereumLogRecord>>) {
        withSpan("onOwnershipEvents") {
            events
                .flatMap { eventConverter.convert(it.record) }
                .filter { event -> OwnershipId.parseId(event.entityId).let { ItemId(it.token, it.tokenId) } !in skipTransferContractTokens }
                .let { delegate.reduceAll(it) }
        }
    }
}
