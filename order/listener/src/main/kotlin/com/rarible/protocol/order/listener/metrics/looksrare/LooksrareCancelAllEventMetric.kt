package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareCancelAllEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.looksrare.event.cancel_all", tag("blockchain", blockchain.value))