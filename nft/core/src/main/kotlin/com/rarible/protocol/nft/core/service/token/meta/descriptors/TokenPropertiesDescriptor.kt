package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.rarible.protocol.nft.core.model.TokenProperties
import scalether.domain.Address

interface TokenPropertiesDescriptor {
    suspend fun resolve(id: Address): TokenProperties?
    fun order(): Int
}
