package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

@Component
@WritingConverter
object ItemIdWritingConverter : Converter<ItemId, String> {
    override fun convert(source: ItemId): String = source.stringValue
}
