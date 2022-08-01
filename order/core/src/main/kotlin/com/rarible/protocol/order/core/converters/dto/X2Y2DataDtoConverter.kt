package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderX2Y2DataDto
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1

object X2Y2DataDtoConverter {

    fun convert(source: OrderX2Y2DataV1): OrderX2Y2DataDto {
        return OrderX2Y2DataDto(
            itemHash = source.itemHash,
            isCollectionOffer = source.isCollectionOffer,
            isBundle = source.isBundle,
            side = source.side
        )
    }
}
