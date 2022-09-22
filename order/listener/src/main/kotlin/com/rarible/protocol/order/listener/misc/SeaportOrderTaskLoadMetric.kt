package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportOrderTaskLoadMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.seaport.order.task.load", tag("blockchain", blockchain.value))