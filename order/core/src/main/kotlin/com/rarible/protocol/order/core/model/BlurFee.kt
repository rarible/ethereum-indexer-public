package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

data class BlurFee(
    val rate: BigInteger,
    val recipient: Address,
)
