package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import java.math.BigInteger

data class BlurTradeDetails(
    val marketId: BigInteger,
    val value: BigInteger,
    val tradeData: Binary
)


