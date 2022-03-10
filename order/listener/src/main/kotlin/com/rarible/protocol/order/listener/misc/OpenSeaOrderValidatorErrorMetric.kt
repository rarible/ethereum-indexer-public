package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class OpenSeaOrderValidatorErrorMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.opensea.validator.error", tag("blockchain", blockchain.value))