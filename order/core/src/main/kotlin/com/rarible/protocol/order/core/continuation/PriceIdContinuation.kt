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

    companion object {
        fun parse(str: String?): PriceIdContinuation? {
            val pair = Continuation.splitBy(str, "_") ?: return null
            val price = pair.first
            val id = pair.second
            return PriceIdContinuation(BigDecimal(price), id)
        }
    }
}
