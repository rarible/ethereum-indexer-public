package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

data class ZeroExFeeData(
    val recipient: Address,
    val paymentTokenAmount: BigInteger,
)
