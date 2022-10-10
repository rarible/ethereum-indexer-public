package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class X2Y2EventLoadMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.x2y2.event.load", tag("blockchain", blockchain.value))