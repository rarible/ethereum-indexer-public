package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class X2Y2OrderLoadErrorMetric(root: String, blockchain: Blockchain): CountingMetric(
    "$root.x2y2.order.load.error", tag("blockchain", blockchain.value)
)
