package com.rarible.protocol.nft.core.metric

import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

abstract class BaseMetrics(
    private val meterRegistry: MeterRegistry
) {

    protected fun tag(blockchain: Blockchain): Tag {
        return ImmutableTag(BLOCKCHAIN, blockchain.name.lowercase())
    }

    protected fun tagStatus(name: String): Tag {
        return ImmutableTag(STATUS, name)
    }

    protected fun increment(name: String, vararg tags: Tag) {
        meterRegistry.counter(name, tags.toList()).increment()
    }

    companion object {
        const val BLOCKCHAIN = "blockchain"
        const val STATUS = "status"
    }
}
