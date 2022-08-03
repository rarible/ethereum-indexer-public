package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareOrderDelayLoadMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.looksrare.order.delay.load", tag("blockchain", blockchain.value))