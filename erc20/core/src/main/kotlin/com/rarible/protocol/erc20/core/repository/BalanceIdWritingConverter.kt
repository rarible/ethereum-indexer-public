package com.rarible.protocol.erc20.core.repository

import com.rarible.protocol.erc20.core.model.BalanceId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

@Component
@WritingConverter
object BalanceIdWritingConverter : Converter<BalanceId, String> {
    override fun convert(source: BalanceId): String = source.stringValue
}
