package com.rarible.protocol.nft.core.converters.model

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.core.model.Part
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object RoyaltyConverter : Converter<PartDto, Part> {
    override fun convert(source: PartDto): Part {
        return Part(
            account = source.account,
            value = source.value
        )
    }
}
