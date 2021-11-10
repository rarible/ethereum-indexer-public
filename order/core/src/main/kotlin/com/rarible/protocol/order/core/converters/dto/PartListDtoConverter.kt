package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.model.Part

object PartListDtoConverter {
    fun convert(source: List<Part>): List<PartDto> {
        return source.map { PartDto(it.account, it.value.value.intValueExact()) }
    }
}
