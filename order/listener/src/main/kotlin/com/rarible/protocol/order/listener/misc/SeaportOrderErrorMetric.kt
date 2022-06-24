package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportOrderErrorMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.seaport.order.error", tag("blockchain", blockchain.value))