package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class OpenSeaConverterErrorMetric(root: String, blockchain: Blockchain): CountingMetric(
    "$root.opensea.converter.error", tag("blockchain", blockchain.value)) {
}