package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.NftOrderDeletedOwnershipDto
import com.rarible.protocol.dto.NftOrderOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOrderOwnershipEventDto
import com.rarible.protocol.dto.NftOrderOwnershipUpdateEventDto
import com.rarible.protocol.nftorder.core.event.OwnershipEvent
import com.rarible.protocol.nftorder.core.event.OwnershipEventDelete
import com.rarible.protocol.nftorder.core.event.OwnershipEventUpdate
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OwnershipEventToDtoConverter : Converter<OwnershipEvent, NftOrderOwnershipEventDto> {

    override fun convert(source: OwnershipEvent): NftOrderOwnershipEventDto {
        return when (source) {
            is OwnershipEventUpdate -> NftOrderOwnershipUpdateEventDto(
                source.id,
                source.ownership.id.decimalStringValue,
                OwnershipToDtoConverter.convert(source.ownership)
            )
            is OwnershipEventDelete -> NftOrderOwnershipDeleteEventDto(
                source.id,
                source.ownershipId.decimalStringValue,
                NftOrderDeletedOwnershipDto(
                    id = source.ownershipId.stringValue,
                    owner = source.ownershipId.owner,
                    token = source.ownershipId.token,
                    tokenId = source.ownershipId.tokenId.value
                )
            )
        }
    }
}

