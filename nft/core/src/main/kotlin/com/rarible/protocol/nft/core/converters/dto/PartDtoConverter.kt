package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.core.model.Part
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object PartDtoConverter : Converter<Part, PartDto> {
    override fun convert(source: Part): PartDto {
        return PartDto(
            account = source.account,
            value = source.value
        )
    }
}
