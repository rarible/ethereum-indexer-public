package com.rarible.protocol.erc20.core.metric

import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.MeterRegistry


class DescriptorMetrics(
    private val blockchain: Blockchain,
    meterRegistry: MeterRegistry,
) : BaseMetrics(meterRegistry) {

    fun onIncoming() {
        increment(LOG_EVENT_INCOMING, tag(blockchain))
    }

    fun onSaved() {
        increment(LOG_EVENT_SAVED, tag(blockchain))
    }

    fun onIgnored() {
        increment(LOG_EVENT_IGNORED, tag(blockchain))
    }

    companion object {
        const val LOG_EVENT_INCOMING = "log_event_incoming"
        const val LOG_EVENT_SAVED = "log_event_saved"
        const val LOG_EVENT_IGNORED = "log_event_ignored"
    }
}
