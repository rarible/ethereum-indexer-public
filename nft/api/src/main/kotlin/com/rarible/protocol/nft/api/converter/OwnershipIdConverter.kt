package com.rarible.protocol.nft.api.converter

import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.core.model.OwnershipId

object OwnershipIdConverter {

    fun convert(source: String): OwnershipId {
        return try {
            OwnershipId.parseId(source)
        } catch (ex: Exception) {
            throw ValidationApiException("Can't parse ownership id '$source', error: ${ex.message}")
        }
    }
}
