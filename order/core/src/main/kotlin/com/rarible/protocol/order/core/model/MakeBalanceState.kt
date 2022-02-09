package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import java.time.Instant

data class MakeBalanceState(
    val value: EthUInt256,
    val lastUpdatedAt: Instant? = null
)