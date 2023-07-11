package com.rarible.protocol.nft.core.service.ownership.reduce.status

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.AbstractCompactEventsReducer
import org.springframework.stereotype.Component

@Component
class CompactOwnershipEventsReducer(
    featureFlags: FeatureFlags,
    properties: NftIndexerProperties.ReduceProperties,
) : AbstractCompactEventsReducer<OwnershipId, OwnershipEvent, Ownership>(featureFlags, properties) {

    override fun compact(events: List<OwnershipEvent>): List<OwnershipEvent> {
        return when (val last = events.last()) {
            is OwnershipEvent.TransferToEvent,
            is OwnershipEvent.TransferFromEvent -> {
                val transferValue = EthUInt256.of(events.sumOf { it.value.value })
                listOf(last.withValue(transferValue))
            }
            is OwnershipEvent.ChangeLazyValueEvent,
            is OwnershipEvent.LazyBurnEvent,
            is OwnershipEvent.LazyTransferToEvent, -> events
        }
    }
}

