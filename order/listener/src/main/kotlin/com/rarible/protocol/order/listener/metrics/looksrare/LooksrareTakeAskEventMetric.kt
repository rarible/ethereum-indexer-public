package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareTakeAskEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.looksrare.event.takeask", tag("blockchain", blockchain.value))

