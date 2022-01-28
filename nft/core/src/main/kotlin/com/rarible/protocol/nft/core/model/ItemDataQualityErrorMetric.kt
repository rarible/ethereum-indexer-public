package com.rarible.protocol.nft.core.model

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class ItemDataQualityErrorMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.item.data.quality.error", tag("blockchain", blockchain.value)
)

