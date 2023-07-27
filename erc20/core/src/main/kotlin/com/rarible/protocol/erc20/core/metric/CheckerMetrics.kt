package com.rarible.protocol.erc20.core.metric

import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.MeterRegistry

class CheckerMetrics(
    private val blockchain: Blockchain,
    meterRegistry: MeterRegistry,
) : BaseMetrics(meterRegistry) {

    fun onIncoming() {
        increment(BALANCE_INCOMING, tag(blockchain))
    }

    fun onCheck() {
        increment(BALANCE_CHECK, tag(blockchain))
    }

    fun onInvalid() {
        increment(BALANCE_INVALID, tag(blockchain))
    }

    companion object {
        const val BALANCE_INCOMING = "balance_incoming"
        const val BALANCE_CHECK = "balance_check"
        const val BALANCE_INVALID = "balance_invalid"
    }
}
