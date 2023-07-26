package com.rarible.protocol.nft.api.service.domain

import com.rarible.protocol.nft.api.model.DomainResolveResult
import com.rarible.protocol.nft.api.model.DomainType

interface DomainResolver {
    val types: List<DomainType>

    fun isValidName(name: String): Boolean

    suspend fun resolve(name: String): DomainResolveResult
}
