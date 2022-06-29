package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportCounterEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.seaport.event.counter", tag("blockchain", blockchain.value))