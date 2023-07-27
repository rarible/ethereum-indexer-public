package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderSudoSwapAmmDataV1Dto
import com.rarible.protocol.dto.SudoSwapCurveTypeDto
import com.rarible.protocol.dto.SudoSwapPoolTypeDto
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.SudoSwapCurveType
import com.rarible.protocol.order.core.model.SudoSwapCurveType.EXPONENTIAL
import com.rarible.protocol.order.core.model.SudoSwapCurveType.LINEAR
import com.rarible.protocol.order.core.model.SudoSwapCurveType.UNKNOWN
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.model.SudoSwapPoolType.NFT
import com.rarible.protocol.order.core.model.SudoSwapPoolType.TOKEN
import com.rarible.protocol.order.core.model.SudoSwapPoolType.TRADE

object SudoSwapAmmDataV1DtoConverter {
    fun convert(source: OrderSudoSwapAmmDataV1): OrderSudoSwapAmmDataV1Dto {
        return OrderSudoSwapAmmDataV1Dto(
            poolAddress = source.poolAddress,
            bondingCurve = source.bondingCurve,
            curveType = convert(source.curveType),
            assetRecipient = source.assetRecipient,
            poolType = convert(source.poolType),
            delta = source.delta,
            fee = source.fee,
            feeDecimal = 18
        )
    }

    fun convert(source: SudoSwapPoolType): SudoSwapPoolTypeDto {
        return when (source) {
            TOKEN -> SudoSwapPoolTypeDto.TOKEN
            NFT -> SudoSwapPoolTypeDto.NFT
            TRADE -> SudoSwapPoolTypeDto.TRADE
        }
    }

    fun convert(source: SudoSwapCurveType): SudoSwapCurveTypeDto {
        return when (source) {
            LINEAR -> SudoSwapCurveTypeDto.LINEAR
            EXPONENTIAL -> SudoSwapCurveTypeDto.EXPONENTIAL
            UNKNOWN -> SudoSwapCurveTypeDto.UNKNOWN
        }
    }
}
