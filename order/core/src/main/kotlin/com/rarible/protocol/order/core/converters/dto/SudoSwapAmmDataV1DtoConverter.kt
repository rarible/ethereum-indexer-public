package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderSudoSwapAmmDataV1Dto
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1

object SudoSwapAmmDataV1DtoConverter {
    fun convert(source: OrderSudoSwapAmmDataV1): OrderSudoSwapAmmDataV1Dto {
        return OrderSudoSwapAmmDataV1Dto(
            contract = source.poolAddress,
        )
    }
}