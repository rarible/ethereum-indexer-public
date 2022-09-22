package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportOrderTaskSaveMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.seaport.order.task.save", tag("blockchain", blockchain.value))