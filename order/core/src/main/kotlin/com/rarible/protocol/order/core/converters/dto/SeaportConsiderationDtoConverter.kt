package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportConsiderationDto
import com.rarible.protocol.order.core.model.SeaportConsideration

object SeaportConsiderationDtoConverter {
    fun convert(source: SeaportConsideration): SeaportConsiderationDto {
        return SeaportConsiderationDto(
            itemType = SeaportItemTypeDtoConverter.convert(source.itemType),
            token = source.token,
            identifierOrCriteria = source.identifierOrCriteria,
            startAmount = source.startAmount,
            endAmount = source.endAmount,
            recipient = source.recipient
        )
    }
}