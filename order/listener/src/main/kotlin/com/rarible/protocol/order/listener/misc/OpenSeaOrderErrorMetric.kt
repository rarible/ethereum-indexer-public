package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class OpenSeaOrderErrorMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.opensea.order.error", tag("blockchain", blockchain.value))

