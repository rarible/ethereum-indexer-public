package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.protocol.nft.core.model.TokenMeta
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenMetaService(
    private val tokenPropertiesService: TokenPropertiesService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO PT-2370 should throw correct exceptions or null
    suspend fun get(id: Address): TokenMeta {
        // TODO PT-2370 remove later
        val properties = tokenPropertiesService.resolve(id) ?: return TokenMeta.EMPTY
        return TokenMeta(properties)
    }
}
