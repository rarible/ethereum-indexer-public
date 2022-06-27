package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportEventErrorMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.seaport.event.error", tag("blockchain", blockchain.value))