package com.rarible.protocol.nft.core.metric

import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.MeterRegistry

class CheckerMetrics(
    private val blockchain: Blockchain,
    meterRegistry: MeterRegistry,
) : BaseMetrics(meterRegistry) {

    fun onIncoming() {
        increment(OWNERSHIPS_INCOMING, tag(blockchain))
    }

    fun onSkipped() {
        increment(OWNERSHIPS_CHECKED, tag(blockchain), tagStatus(SKIPPED_TAG))
    }

    fun onSuccess() {
        increment(OWNERSHIPS_CHECKED, tag(blockchain), tagStatus(SUCCESS_TAG))
    }

    fun onFail() {
        increment(OWNERSHIPS_CHECKED, tag(blockchain), tagStatus(FAIL_TAG))
    }

    companion object {
        const val OWNERSHIPS_INCOMING = "ownerships_incoming"
        const val OWNERSHIPS_CHECKED = "ownerships_checked"
        const val SKIPPED_TAG = "skipped"
        const val SUCCESS_TAG = "success"
        const val FAIL_TAG = "fail"
    }
}
