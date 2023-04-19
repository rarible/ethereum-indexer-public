package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.service.item.meta.TOKEN_META_CAPTURE_SPAN_TYPE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class TokenMetaService(
    private val tokenPropertiesService: TokenPropertiesService,
) {

    // TODO PT-2370 should throw correct exceptions or null
    suspend fun get(id: Address): TokenMeta {
        val properties = tokenPropertiesService.resolve(id)

        if (properties == null) return TokenMeta.EMPTY // TODO PT-2370 remove later

        return TokenMeta(properties)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TokenMetaService::class.java)
    }
}
