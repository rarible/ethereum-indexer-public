package com.rarible.protocol.nft.core.model

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class ItemDataQualityJobRunMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.item.data.quality.job.run", tag("blockchain", blockchain.value)
)