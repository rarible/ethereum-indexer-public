package com.rarible.protocol.erc20.core.metric

import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

abstract class BaseMetrics(
    private val meterRegistry: MeterRegistry
) {
    protected fun tag(blockchain: Blockchain): Tag {
        return ImmutableTag("blockchain", blockchain.name.lowercase())
    }

    protected fun tag(name: String, value: String): Tag {
        return ImmutableTag(name, value)
    }

    protected fun increment(name: String, vararg tags: Tag) {
        meterRegistry.counter(name, tags.toList()).increment()
    }
}
