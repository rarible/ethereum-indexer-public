package com.rarible.protocol.nft.core.service

import com.rarible.blockchain.scanner.ethereum.reduce.CompactEventsReducer
import com.rarible.blockchain.scanner.ethereum.model.EthereumEntityEvent
import com.rarible.core.entity.reducer.model.Entity
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags

abstract class AbstractCompactEventsReducer<Id, Event : EthereumEntityEvent<Event>, E : Entity<Id, Event, E>>(
    private val featureFlags: FeatureFlags,
    private val properties: NftIndexerProperties.ReduceProperties,
) : CompactEventsReducer<Id, Event, E>() {

    override suspend fun reduce(entity: E, event: Event): E {
        if (featureFlags.compactRevertableEvents.not() ||
            entity.revertableEvents.size < properties.maxRevertableEventsAmount) {
            return entity
        }
        return super.reduce(entity, event)
    }
}
