package com.rarible.protocol.nft.core.service

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withTransaction
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.EntityEventListeners
import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.service.item.reduce.ItemEventReduceService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipEventReduceService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class CompositeEntityEventListener(
    private val itemEventReduceService: ItemEventReduceService,
    private val ownershipEventReduceService: OwnershipEventReduceService,
    properties: NftIndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo
) : EntityEventListener {

    override val id: String = EntityEventListeners.itemHistoryListenerId(environmentInfo.name, properties.blockchain)

    override val groupId: SubscriberGroup = SubscriberGroups.ITEM_HISTORY

    override suspend fun onEntityEvents(events: List<LogRecordEvent<ReversedEthereumLogRecord>>) {
        withTransaction("onEntityEvents", labels = listOf("size" to events.size)) {
            coroutineScope {
                listOf(
                    async { itemEventReduceService.onEntityEvents(events) },
                    async { ownershipEventReduceService.onEntityEvents(events) }
                ).awaitAll()
            }
        }
    }
}