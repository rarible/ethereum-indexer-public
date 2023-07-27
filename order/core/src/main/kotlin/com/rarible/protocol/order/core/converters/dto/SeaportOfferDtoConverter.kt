package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportOfferDto
import com.rarible.protocol.order.core.model.SeaportOffer

object SeaportOfferDtoConverter {
    fun convert(source: SeaportOffer): SeaportOfferDto {
        return SeaportOfferDto(
            itemType = SeaportItemTypeDtoConverter.convert(source.itemType),
            token = source.token,
            identifierOrCriteria = source.identifier,
            startAmount = source.startAmount,
            endAmount = source.endAmount
        )
    }
}
