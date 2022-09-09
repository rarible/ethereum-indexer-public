package com.rarible.protocol.order.core.model

import java.math.BigInteger

data class SudoSwapBuyInfo(
    val newSpotPrice: BigInteger,
    val newDelta: BigInteger,
    val inputValue: BigInteger,
    val protocolFee: BigInteger
) {
    companion object {
        val ZERO = SudoSwapBuyInfo(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)
    }
}