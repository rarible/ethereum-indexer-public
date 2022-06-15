package com.rarible.protocol.order.core.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class ApprovalNotFoundMetric(root: String, blockchain: Blockchain): CountingMetric("$root.approval.not.found", tag("blockchain", blockchain.value))
