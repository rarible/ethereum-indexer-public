package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class OpenSeaOrderSaveMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.opensea.order.save", tag("blockchain", blockchain.value))

