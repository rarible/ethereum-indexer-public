package com.rarible.protocol.order.core.metric

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class RaribleOrderSaveMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.rarible.order.save", tag("blockchain", blockchain.value))
