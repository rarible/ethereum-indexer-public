package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.LongGaugeMetric
import com.rarible.ethereum.domain.Blockchain

class SeaportOrderDelayMetric(root: String, blockchain: Blockchain) : LongGaugeMetric(
    "$root.seaport.order.delay", tag("blockchain", blockchain.value))