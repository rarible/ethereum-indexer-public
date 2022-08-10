package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.LongGaugeMetric
import com.rarible.ethereum.domain.Blockchain

class X2Y2OrderDelayMetric(root: String, blockchain: Blockchain) : LongGaugeMetric(
    "$root.x2y2.order.delay", tag("blockchain", blockchain.value))