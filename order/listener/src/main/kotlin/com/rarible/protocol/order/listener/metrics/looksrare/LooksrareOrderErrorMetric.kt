package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareOrderErrorMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.looksrare.order.error", tag("blockchain", blockchain.value))

