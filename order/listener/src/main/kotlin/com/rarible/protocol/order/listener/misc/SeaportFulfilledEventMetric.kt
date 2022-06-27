package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportFulfilledEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.seaport.event.fulfilled", tag("blockchain", blockchain.value))