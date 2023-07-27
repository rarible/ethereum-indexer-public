package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.model.Part

object PartListDtoConverter {
    fun convert(source: List<Part>): List<PartDto> {
        return source.map { convert(it) }
    }

    fun convert(source: Part): PartDto {
        return PartDto(source.account, source.value.value.intValueExact())
    }
}
