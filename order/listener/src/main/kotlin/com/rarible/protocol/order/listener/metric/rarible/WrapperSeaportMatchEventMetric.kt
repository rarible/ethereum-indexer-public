package com.rarible.protocol.order.listener.metric.rarible

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class WrapperSeaportMatchEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.wrapper.seaport.event.match", tag("blockchain", blockchain.value))
