package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class X2Y2OrderSaveMetric(root: String, blockchain: Blockchain): CountingMetric(
    "$root.x2y2.order.save", tag("blockchain", blockchain.value)
)
