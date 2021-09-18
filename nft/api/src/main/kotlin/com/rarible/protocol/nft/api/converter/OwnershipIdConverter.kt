package com.rarible.protocol.nft.api.converter

import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OwnershipIdConverter : Converter<String, OwnershipId> {
    override fun convert(source: String): OwnershipId {
        return try {
            OwnershipId.parseId(source)
        } catch (ex: Exception) {
            throw ValidationApiException("Can't parse ownership id '$source', error: ${ex.message}")
        }
    }
}