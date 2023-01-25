package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.model.Ownership
import java.util.*

object OwnershipEventDtoFromOwnershipConverter {

    fun convert(source: Ownership): NftOwnershipEventDto {
        return if (source.deleted) {
            convertToDeleteEvent(source)
        } else {
            convertToUpdateEvent(source)
        }
    }

    fun convertToDeleteEvent(source: Ownership): NftOwnershipEventDto {
        return NftOwnershipDeleteEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = source.id.decimalStringValue,
            ownership = DeletedOwnershipDtoConverter.convert(source.id),
            deletedOwnership = OwnershipDtoConverter.convert(source)
        )
    }

    fun convertToUpdateEvent(source: Ownership): NftOwnershipEventDto {
        return NftOwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = source.id.decimalStringValue,
            ownership = OwnershipDtoConverter.convert(source)
        )
    }

}
