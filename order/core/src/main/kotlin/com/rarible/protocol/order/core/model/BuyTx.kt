package com.rarible.protocol.order.core.model

import java.math.BigInteger

data class BuyTx(
    val from: String,
    val to: String,
    val value: BigInteger,
    val data: String,
)
