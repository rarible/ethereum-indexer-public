package com.rarible.protocol.nft.core.model

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class IncomeBurnActionMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.action.burn.income", tag("blockchain", blockchain.value)
)
