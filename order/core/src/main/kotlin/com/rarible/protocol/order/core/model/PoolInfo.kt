package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

data class PoolInfo(
    val collection: Address,
    val curve: Address,
    val spotPrice: BigInteger,
    val delta: BigInteger,
)