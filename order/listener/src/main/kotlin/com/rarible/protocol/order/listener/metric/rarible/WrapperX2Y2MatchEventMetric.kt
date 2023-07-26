package com.rarible.protocol.order.listener.metric.rarible

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class WrapperX2Y2MatchEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.wrapper.x2y2.event.match", tag("blockchain", blockchain.value))
