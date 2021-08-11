package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nftorder.core.model.Part
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object PartDtoConverter : Converter<List<PartDto>, List<Part>> {

    override fun convert(dto: List<PartDto>): List<Part> {
        return dto.map {
            Part(it.account, it.value)
        }
    }
}