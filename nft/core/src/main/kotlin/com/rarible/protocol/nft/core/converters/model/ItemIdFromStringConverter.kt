package com.rarible.protocol.nft.core.converters.model

import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ItemIdFromStringConverter : Converter<String, ItemId> {
    override fun convert(source: String): ItemId {
        return ItemId.parseId(source)
    }
}