package com.rarible.protocol.order.core.model

import java.math.BigInteger

sealed class SudoSwapCurveInfo {
    abstract val newSpotPrice: BigInteger
    abstract val newDelta: BigInteger
}

data class SudoSwapPurchaseValue(
    override val newSpotPrice: BigInteger,
    override val newDelta: BigInteger,
    val value: BigInteger,
) : SudoSwapCurveInfo()

sealed class SudoSwapPurchaseInfo : SudoSwapCurveInfo() {
    abstract val protocolFee: BigInteger
}

data class SudoSwapBuyInfo(
    override val newSpotPrice: BigInteger,
    override val newDelta: BigInteger,
    override val protocolFee: BigInteger,
    val inputValue: BigInteger,
) : SudoSwapPurchaseInfo() {
    companion object {
        val ZERO = SudoSwapBuyInfo(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)
    }
}

data class SudoSwapSellInfo(
    override val newSpotPrice: BigInteger,
    override val newDelta: BigInteger,
    override val protocolFee: BigInteger,
    val outputValue: BigInteger,
) : SudoSwapPurchaseInfo() {
    companion object {
        val ZERO = SudoSwapSellInfo(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)
    }
}
