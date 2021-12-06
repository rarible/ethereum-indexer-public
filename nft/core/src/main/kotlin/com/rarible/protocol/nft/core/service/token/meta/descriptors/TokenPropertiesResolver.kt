package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.rarible.protocol.nft.core.model.TokenProperties
import scalether.domain.Address

interface TokenPropertiesResolver {
    suspend fun resolve(id: Address): TokenProperties?
    val order: Int
}
