package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.stereotype.Component

@Component
@ReadingConverter
object ItemIdReadingConverter : Converter<String, ItemId> {
    override fun convert(source: String): ItemId = ItemId.parseId(source)
}
