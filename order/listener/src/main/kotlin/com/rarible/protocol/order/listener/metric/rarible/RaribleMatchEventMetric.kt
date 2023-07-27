package com.rarible.protocol.order.listener.metric.rarible

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class RaribleMatchEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.rarible.event.match", tag("blockchain", blockchain.value))
