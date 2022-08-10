package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.LongGaugeMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareOrderDelayMetric(root: String, blockchain: Blockchain) : LongGaugeMetric(
    "$root.looksrare.order.delay", tag("blockchain", blockchain.value))

