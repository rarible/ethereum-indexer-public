package com.rarible.protocol.order.core.model

import java.math.BigDecimal
import java.time.Instant

data class OrderPriceHistoryRecord(
    val date: Instant,
    val makeValue: BigDecimal,
    val takeValue: BigDecimal
)