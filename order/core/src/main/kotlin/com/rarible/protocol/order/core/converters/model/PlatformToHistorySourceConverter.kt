package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Platform

object PlatformToHistorySourceConverter {
    fun convert(source: Platform): HistorySource {
        return source.toHistorySource()
    }
}
