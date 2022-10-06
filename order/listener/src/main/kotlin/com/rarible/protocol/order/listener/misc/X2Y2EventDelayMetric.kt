package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.LongGaugeMetric
import com.rarible.ethereum.domain.Blockchain

class X2Y2EventDelayMetric(root: String, blockchain: Blockchain) : LongGaugeMetric(
    "$root.x2y2.event.delay", tag("blockchain", blockchain.value))