package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class X2Y2OffChainOrderCancelMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.x2y2.event.order.offchain.cancel", tag("blockchain", blockchain.value)
)