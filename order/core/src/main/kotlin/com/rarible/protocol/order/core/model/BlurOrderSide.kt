package com.rarible.protocol.order.core.model

import java.math.BigInteger

enum class BlurOrderSide(val value: BigInteger) {
    BUY(BigInteger.ZERO),
    SELL(BigInteger.ONE)
    ;

    companion object {
        private val VALUES = values().associateBy { it.value }

        fun fromValue(value: BigInteger): BlurOrderSide {
            return VALUES[value] ?: throw IllegalArgumentException("Unsupported value $value")
        }
    }
}
