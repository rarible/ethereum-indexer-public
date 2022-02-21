package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import java.time.Instant

data class MakerNonce(
    val historyId: String,
    val nonce: EthUInt256,
    val timestamp: Instant
)