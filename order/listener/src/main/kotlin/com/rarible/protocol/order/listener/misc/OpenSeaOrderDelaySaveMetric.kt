package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class OpenSeaOrderDelaySaveMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.opensea.order.delay.save", tag("blockchain", blockchain.value))