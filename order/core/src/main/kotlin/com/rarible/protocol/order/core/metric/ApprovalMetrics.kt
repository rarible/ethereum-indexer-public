package com.rarible.protocol.order.core.metric

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.core.model.Platform
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class ApprovalMetrics(
    private val blockchain: Blockchain,
    meterRegistry: MeterRegistry,
) : BaseOrderMetrics(meterRegistry) {

    fun onApprovalEventMiss(platform: Platform) {
        increment(APPROVAL_EVENT_MISS, tag(blockchain), tag(platform))
    }

    fun onApprovalOnChainCheck(platform: Platform, result: Boolean) {
        increment(APPROVAL_ON_CHAIN_CHECK, tag(blockchain), tag(platform), tag("approval", result.toString()))
    }

    private companion object {
        const val APPROVAL_EVENT_MISS = "approval_event_miss"
        const val APPROVAL_ON_CHAIN_CHECK = "approval_on_chain_check"
    }
}
