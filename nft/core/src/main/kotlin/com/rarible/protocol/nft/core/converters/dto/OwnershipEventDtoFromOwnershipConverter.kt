package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.model.Ownership
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.*

@Component
object OwnershipEventDtoFromOwnershipConverter : Converter<Ownership, NftOwnershipEventDto> {
    override fun convert(source: Ownership): NftOwnershipEventDto {
        return if (source.deleted) {
            NftOwnershipDeleteEventDto(
                UUID.randomUUID().toString(),
                source.id.decimalStringValue,
                DeletedOwnershipDtoConverter.convert(source.id)
            )
        } else {
            NftOwnershipUpdateEventDto(
                UUID.randomUUID().toString(),
                source.id.decimalStringValue,
                OwnershipDtoConverter.convert(source)
            )
        }
    }
}
