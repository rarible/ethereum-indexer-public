package com.rarible.protocol.nft.core.metric

import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.MeterRegistry


class CheckerMetrics(
    private val blockchain: Blockchain,
    meterRegistry: MeterRegistry,
) : BaseMetrics(meterRegistry) {

    fun onIncoming() {
        increment(OWNERSHIP_CHECK, tag(blockchain), tagStatus(INCOMING_TAG))
    }

    fun onSkipped() {
        increment(OWNERSHIP_CHECK, tag(blockchain), tagStatus(SKIPPED_TAG))
    }

    fun onSuccess() {
        increment(OWNERSHIP_CHECK, tag(blockchain), tagStatus(SUCCESS_TAG))
    }

    fun onFail() {
        increment(OWNERSHIP_CHECK, tag(blockchain), tagStatus(FAIL_TAG))
    }

    companion object {
        const val OWNERSHIP_CHECK = "ownership_check"
        const val INCOMING_TAG = "incoming"
        const val SKIPPED_TAG = "skipped"
        const val SUCCESS_TAG = "success"
        const val FAIL_TAG = "fail"
    }
}
