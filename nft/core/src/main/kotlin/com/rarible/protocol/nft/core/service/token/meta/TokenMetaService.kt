package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.service.item.meta.MediaMetaService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenMetaService(
    private val tokenPropertiesService: TokenPropertiesService,
    private val mediaMetaService: MediaMetaService
) {

    suspend fun get(id: Address): TokenMeta {
        val properties = tokenPropertiesService.resolve(id)
        return properties?.let { TokenMeta(
            name = it.name,
            description = it.description,
            image = it.image,
            imageMeta = it.image?.let { mediaMetaService.getMediaMeta(it) },
            external_link = it.external_link,
            fee_recipient = it.fee_recipient,
            seller_fee_basis_points = it.seller_fee_basis_points
        ) } ?: TokenMeta.EMPTY
    }

    suspend fun reset(id: Address) {

    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TokenMetaService::class.java)
    }
}
