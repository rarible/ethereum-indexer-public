package com.rarible.protocol.order.core.continuation

import java.math.BigDecimal

data class PriceIdContinuation(
    val price: BigDecimal?,
    val id: String
) : Continuation {

    override fun toString(): String {
        return "${price.orDefault()}_${id}"
    }

    private fun BigDecimal?.orDefault(): BigDecimal = this ?: BigDecimal.ZERO
}
