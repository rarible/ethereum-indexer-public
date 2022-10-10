package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class X2Y2OrderCancelEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.x2y2.event.order.cancel", tag("blockchain", blockchain.value)
)

