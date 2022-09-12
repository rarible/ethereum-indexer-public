package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

sealed class PoolData {
    abstract val poolAddress: Address
    abstract fun toOrderData(): OrderAmmData
}

data class SudoSwapPoolDataV1(
    override val poolAddress: Address,
    val bondingCurve: Address,
    val curveType: SudoSwapCurveType,
    val assetRecipient: Address,
    val factory: Address,
    val poolType: SudoSwapPoolType,
    val spotPrice: BigInteger,
    val delta: BigInteger,
    val fee: BigInteger
): PoolData() {
    override fun toOrderData(): OrderSudoSwapAmmDataV1 {
        return OrderSudoSwapAmmDataV1(
            poolAddress = poolAddress,
            bondingCurve = bondingCurve,
            factory = factory,
            curveType = curveType,
            assetRecipient = assetRecipient,
            poolType = poolType,
            spotPrice = spotPrice,
            delta = delta,
            fee = fee
        )
    }
}