package com.rarible.protocol.order.listener.metric.rarible

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class RaribleCancelEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.rarible.event.cancel", tag("blockchain", blockchain.value))
