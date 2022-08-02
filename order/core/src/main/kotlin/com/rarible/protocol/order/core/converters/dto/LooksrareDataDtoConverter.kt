package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderLooksrareDataV1Dto
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1

object LooksrareDataDtoConverter {
    fun convert(source: OrderLooksrareDataV1): OrderLooksrareDataV1Dto {
        return OrderLooksrareDataV1Dto(
            minPercentageToAsk = source.minPercentageToAsk,
            nonce = source.nonce,
            params = source.params,
            strategy = source.strategy
        )
    }
}