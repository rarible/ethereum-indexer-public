package com.rarible.protocol.order.core.metric

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.core.model.Platform
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

abstract class BaseMetrics(
    protected val meterRegistry: MeterRegistry
) {
    protected fun tag(blockchain: Blockchain): Tag {
        return tag("blockchain", blockchain.name.lowercase())
    }

    protected fun tag(platform: Platform): Tag {
        return tag("platform", platform.name.lowercase())
    }

    protected fun tag(key: String, value: String): Tag {
        return ImmutableTag(key, value)
    }

    protected fun increment(name: String, vararg tags: Tag) {
        return meterRegistry.counter(name, tags.toList()).increment()
    }
}