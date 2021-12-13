package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Platform
import org.springframework.core.convert.converter.Converter

object PlatformToHistorySourceConverter : Converter<Platform, HistorySource> {
    override fun convert(source: Platform): HistorySource {
        return when (source) {
            Platform.RARIBLE -> HistorySource.RARIBLE
            Platform.OPEN_SEA -> HistorySource.OPEN_SEA
            Platform.CRYPTO_PUNKS -> HistorySource.CRYPTO_PUNKS
        }
    }
}
