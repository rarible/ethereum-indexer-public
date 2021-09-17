package com.rarible.protocol.nft.api.converter

import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ItemIdConverter : Converter<String, ItemId> {
    override fun convert(source: String): ItemId {
        return try {
            ItemId.parseId(source)
        } catch (ex: Exception) {
            throw ValidationApiException("Can't parse item id '$source', error: ${ex.message}")
        }
    }
}

