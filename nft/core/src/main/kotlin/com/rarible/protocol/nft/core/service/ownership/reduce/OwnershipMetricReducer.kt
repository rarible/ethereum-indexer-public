package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.AbstractMetricReducer
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class OwnershipMetricReducer(
    properties: NftIndexerProperties,
    meterRegistry: MeterRegistry,
) : AbstractMetricReducer<OwnershipEvent, Ownership>(properties, meterRegistry, "ownership") {

    override fun getMetricName(event: OwnershipEvent): String {
        return when (event) {
            is OwnershipEvent.TransferFromEvent -> "transfer_from"
            is OwnershipEvent.TransferToEvent -> "transfer_to"
            is OwnershipEvent.ChangeLazyValueEvent -> "change_lazy"
            is OwnershipEvent.LazyTransferToEvent -> "lazy_transfer_to"
        }
    }
}
