package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withTransaction
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.converters.model.OwnershipEventConverter
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.EntityEventListener
import org.springframework.stereotype.Component

@Component
class OwnershipEventReduceService(
    entityService: OwnershipUpdateService,
    entityIdService: OwnershipIdService,
    templateProvider: OwnershipTemplateProvider,
    reducer: OwnershipReducer,
    private val eventConverter: OwnershipEventConverter,
    properties: NftIndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo
) : EntityEventListener {

    private val skipTransferContractTokens = properties.scannerProperties.skipTransferContractTokens.map(ItemIdFromStringConverter::convert)
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    override val id: String = EntityEventListeners.ownershipHistoryListenerId(environmentInfo.name, properties.blockchain)

    override val subscriberGroup: SubscriberGroup = SubscriberGroups.ITEM_HISTORY

    suspend fun reduce(events: List<OwnershipEvent>) {
        delegate.reduceAll(events)
    }

    override suspend fun onEntityEvents(events: List<LogRecordEvent<ReversedEthereumLogRecord>>) {
        withTransaction("onOwnershipEvents", labels = listOf("size" to events.size)) {
            events
                .flatMap { eventConverter.convert(it.record) }
                .filter { event -> OwnershipId.parseId(event.entityId).let { ItemId(it.token, it.tokenId) } !in skipTransferContractTokens }
                .let { delegate.reduceAll(it) }
        }
    }
}
