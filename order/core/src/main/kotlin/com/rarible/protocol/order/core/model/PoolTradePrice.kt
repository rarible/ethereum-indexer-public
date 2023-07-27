package com.rarible.protocol.order.core.model

import java.math.BigDecimal
import java.math.BigInteger

data class PoolTradePrice(
    val price: BigInteger,
    val priceValue: BigDecimal,
    val priceUsd: BigDecimal?,
)
