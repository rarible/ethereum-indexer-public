package com.rarible.protocol.nft.core.service.token

import com.google.common.cache.CacheBuilder
import com.rarible.protocol.nft.core.model.TokenStandard
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenStandardCache(
    @Value("\${nft.token.cache.max.size:10000}")
    private val cacheMaxSize: Long
) {

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(cacheMaxSize)
        .build<Address, TokenStandard>()

    // TODO ideally this cache should be in TokenService
    fun set(address: Address, standard: TokenStandard): TokenStandard {
        cache.put(address, standard)
        return standard
    }

    fun get(address: Address): TokenStandard? {
        return cache.getIfPresent(address)
    }
}
