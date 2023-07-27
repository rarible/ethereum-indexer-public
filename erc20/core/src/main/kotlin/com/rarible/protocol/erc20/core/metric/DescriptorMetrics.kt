package com.rarible.protocol.erc20.core.metric

import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.MeterRegistry

class DescriptorMetrics(
    private val blockchain: Blockchain,
    meterRegistry: MeterRegistry,
) : BaseMetrics(meterRegistry) {

    fun onSaved() {
        increment(LOG_EVENT_SAVED, tag(blockchain))
    }

    fun onSkipped(reason: String) {
        increment(LOG_EVENT_SKIPPED, tag(blockchain), tag(TAG_REASON, reason))
    }

    companion object {
        const val LOG_EVENT_SAVED = "log_event_saved"
        const val LOG_EVENT_SKIPPED = "log_event_skipped"
        const val TAG_REASON = "reason"
    }
}
