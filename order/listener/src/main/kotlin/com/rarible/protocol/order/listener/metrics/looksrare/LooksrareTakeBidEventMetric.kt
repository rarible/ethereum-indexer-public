package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareTakeBidEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.looksrare.event.takebid", tag("blockchain", blockchain.value))