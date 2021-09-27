package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nftorder.core.model.Part

object PartDtoConverter {

    fun convert(dto: List<PartDto>): List<Part> {
        return dto.map {
            Part(it.account, it.value)
        }
    }
}