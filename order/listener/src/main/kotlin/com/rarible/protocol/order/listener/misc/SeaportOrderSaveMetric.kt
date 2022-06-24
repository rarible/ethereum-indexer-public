package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportOrderSaveMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.seaport.order.save", tag("blockchain", blockchain.value))