package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import java.util.*

object OwnershipEventDtoFromOwnershipConverter {

    fun convert(source: Ownership, event: OwnershipEvent? = null): NftOwnershipEventDto {
        return if (source.deleted) {
            convertToDeleteEvent(source, event)
        } else {
            convertToUpdateEvent(source, event)
        }
    }

    private fun convertToDeleteEvent(source: Ownership, event: OwnershipEvent?): NftOwnershipEventDto {
        return NftOwnershipDeleteEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = source.id.decimalStringValue,
            ownership = DeletedOwnershipDtoConverter.convert(source.id),
            deletedOwnership = OwnershipDtoConverter.convert(source),
            eventTimeMarks = blockchainEventMark(event?.log?.blockTimestamp)
        )
    }

    private fun convertToUpdateEvent(source: Ownership, event: OwnershipEvent?): NftOwnershipEventDto {
        return NftOwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = source.id.decimalStringValue,
            ownership = OwnershipDtoConverter.convert(source),
            eventTimeMarks = blockchainEventMark(event?.log?.blockTimestamp)
        )
    }

}
