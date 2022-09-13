package com.rarible.protocol.order.core.model

import java.math.BigInteger

sealed class SudoSwapCurveInfo {
    abstract val newSpotPrice: BigInteger
    abstract val newDelta: BigInteger
    abstract val protocolFee: BigInteger
}

data class SudoSwapBuyInfo(
    override val newSpotPrice: BigInteger,
    override val newDelta: BigInteger,
    val inputValue: BigInteger,
    override val protocolFee: BigInteger
) : SudoSwapCurveInfo() {
    companion object {
        val ZERO = SudoSwapBuyInfo(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)
    }
}

data class SudoSwapSellInfo(
    override val newSpotPrice: BigInteger,
    override val newDelta: BigInteger,
    val outputValue: BigInteger,
    override val protocolFee: BigInteger
) : SudoSwapCurveInfo() {
    companion object {
        val ZERO = SudoSwapSellInfo(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)
    }
}

