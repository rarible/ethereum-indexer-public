package com.rarible.protocol.order.listener.metric

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class OrderExpiredMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.order.expired", tag("blockchain", blockchain.value))
