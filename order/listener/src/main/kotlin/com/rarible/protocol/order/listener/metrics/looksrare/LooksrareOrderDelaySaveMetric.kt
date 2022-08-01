package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareOrderDelaySaveMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.looksrare.order.delay.save", tag("blockchain", blockchain.value))