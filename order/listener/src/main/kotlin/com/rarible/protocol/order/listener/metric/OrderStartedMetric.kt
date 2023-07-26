package com.rarible.protocol.order.listener.metric

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class OrderStartedMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.order.started", tag("blockchain", blockchain.value))
