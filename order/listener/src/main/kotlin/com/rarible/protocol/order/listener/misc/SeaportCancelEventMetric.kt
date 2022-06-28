package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportCancelEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.seaport.event.cancel", tag("blockchain", blockchain.value))