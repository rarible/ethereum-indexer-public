package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import java.util.UUID

object OwnershipEventDtoFromOwnershipConverter {

    fun convert(source: Ownership, event: OwnershipEvent? = null): NftOwnershipEventDto {

        val markName = "indexer-out_nft"
        val marks = event?.eventTimeMarks?.addOut("nft")?.toDto()
            ?: event?.log?.blockTimestamp?.let { blockchainEventMark(markName, it) }
            ?: offchainEventMark(markName)

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
