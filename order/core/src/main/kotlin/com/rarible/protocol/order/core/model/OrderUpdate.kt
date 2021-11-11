package com.rarible.protocol.order.core.model

import java.math.BigDecimal
import java.util.*

data class OrderUpdate(
    val makeValue: BigDecimal,
    val takeValue: BigDecimal,

    val fee: Int,

    val makePriceEth: BigDecimal?,
    val takePriceEth: BigDecimal?,

    val updateAt: Date,
    val version: Long
)