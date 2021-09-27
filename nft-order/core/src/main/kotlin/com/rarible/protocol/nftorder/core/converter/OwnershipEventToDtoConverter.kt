package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.NftOrderDeletedOwnershipDto
import com.rarible.protocol.dto.NftOrderOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOrderOwnershipEventDto
import com.rarible.protocol.dto.NftOrderOwnershipUpdateEventDto
import com.rarible.protocol.nftorder.core.event.OwnershipEvent
import com.rarible.protocol.nftorder.core.event.OwnershipEventDelete
import com.rarible.protocol.nftorder.core.event.OwnershipEventUpdate

object OwnershipEventToDtoConverter {

    fun convert(source: OwnershipEvent): NftOrderOwnershipEventDto {
        return when (source) {
            is OwnershipEventUpdate -> NftOrderOwnershipUpdateEventDto(
                eventId = source.id,
                ownershipId = source.ownership.id,
                ownership = source.ownership
            )
            is OwnershipEventDelete -> NftOrderOwnershipDeleteEventDto(
                eventId = source.id,
                ownershipId = source.ownershipId.decimalStringValue,
                ownership = NftOrderDeletedOwnershipDto(
                    id = source.ownershipId.stringValue,
                    owner = source.ownershipId.owner,
                    token = source.ownershipId.token,
                    tokenId = source.ownershipId.tokenId.value
                )
            )
        }
    }
}

