package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.core.model.Part

object PartDtoConverter {

    fun convert(source: Part): PartDto {
        return PartDto(
            account = source.account,
            value = source.value
        )
    }
}
