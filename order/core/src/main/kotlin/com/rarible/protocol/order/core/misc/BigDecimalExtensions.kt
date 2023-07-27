package com.rarible.protocol.order.core.misc

import java.math.BigDecimal
import java.math.MathContext

fun BigDecimal.safeDivide128(value: BigDecimal): BigDecimal {
    return if (value == BigDecimal.ZERO) BigDecimal.ZERO else this.divide(value, MathContext.DECIMAL128)
}
