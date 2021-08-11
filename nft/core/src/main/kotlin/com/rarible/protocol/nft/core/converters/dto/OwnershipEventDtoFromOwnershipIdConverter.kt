package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.*

@Component
object OwnershipEventDtoFromOwnershipIdConverter : Converter<OwnershipId, NftOwnershipEventDto> {
    override fun convert(source: OwnershipId): NftOwnershipEventDto {
        return NftOwnershipDeleteEventDto(
            UUID.randomUUID().toString(),
            source.decimalStringValue,
            DeletedOwnershipDtoConverter.convert(source)
        )
    }
}