package com.rarible.protocol.order.listener.metrics.looksrare

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class LooksrareCancelOrdersEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.looksrare.event.cancel_orders", tag("blockchain", blockchain.value))

