package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.MediaMetaService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.TOKEN_META_CAPTURE_SPAN_TYPE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class TokenMetaService(
    private val tokenPropertiesService: TokenPropertiesService,
    private val mediaMetaService: MediaMetaService
) {

    suspend fun get(id: Address): TokenMeta {
        val properties = tokenPropertiesService.resolve(id)
        return if (properties == null) TokenMeta.EMPTY else {
            TokenMeta(
                properties = properties,
                contentMeta = properties.image?.let { mediaMetaService.getMediaMeta(it) }
            )
        }
    }

    suspend fun reset(id: Address) {
        tokenPropertiesService.reset(id)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TokenMetaService::class.java)
    }
}
