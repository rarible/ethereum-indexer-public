package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareOrderSaveMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.looksrare.order.save", tag("blockchain", blockchain.value))

