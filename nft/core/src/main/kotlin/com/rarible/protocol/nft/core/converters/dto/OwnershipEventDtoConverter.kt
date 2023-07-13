package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.misc.addIndexerOut
import com.rarible.protocol.nft.core.misc.toDto
import com.rarible.protocol.nft.core.model.Ownership
import java.util.UUID

object OwnershipEventDtoConverter {

    fun convert(source: Ownership, eventTimeMarks: EventTimeMarks): NftOwnershipEventDto {

        val marks = eventTimeMarks.addIndexerOut().toDto()

        return if (source.deleted) {
            NftOwnershipDeleteEventDto(
                eventId = UUID.randomUUID().toString(),
                ownershipId = source.id.decimalStringValue,
                ownership = DeletedOwnershipDtoConverter.convert(source.id),
                deletedOwnership = OwnershipDtoConverter.convert(source),
                eventTimeMarks = marks,
                blockNumber = source.blockNumber
            )
        } else {
            NftOwnershipUpdateEventDto(
                eventId = UUID.randomUUID().toString(),
                ownershipId = source.id.decimalStringValue,
                ownership = OwnershipDtoConverter.convert(source),
                eventTimeMarks = marks,
                blockNumber = source.blockNumber
            )
        }
    }
}
