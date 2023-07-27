package com.rarible.protocol.nft.core.model

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class ErrorBurnActionMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.action.burn.error", tag("blockchain", blockchain.value)
)
