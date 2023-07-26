package com.rarible.protocol.order.core.metric

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.core.model.Platform
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

abstract class BaseOrderMetrics(
    protected val meterRegistry: MeterRegistry
) {

    protected fun tag(blockchain: Blockchain): Tag {
        return tag("blockchain", blockchain.name.lowercase())
    }

    protected fun tag(platform: Platform): Tag {
        return tag("platform", platform.name.lowercase())
    }

    protected fun status(status: String): Tag {
        return tag("status", status)
    }

    protected fun type(type: String): Tag {
        return tag("type", type)
    }

    protected fun reason(reason: String): Tag {
        return tag("reason", reason)
    }

    protected fun tag(key: String, value: String): Tag {
        return ImmutableTag(key, value)
    }

    protected fun increment(name: String, vararg tags: Tag) {
        return meterRegistry.counter(name, tags.toList()).increment()
    }
}
