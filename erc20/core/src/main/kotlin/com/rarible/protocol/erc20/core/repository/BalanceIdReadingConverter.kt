package com.rarible.protocol.erc20.core.repository

import com.rarible.protocol.erc20.core.model.BalanceId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.stereotype.Component

@Component
@ReadingConverter
object BalanceIdReadingConverter : Converter<String, BalanceId> {
    override fun convert(source: String): BalanceId = BalanceId.parseId(source)
}
