package com.rarible.protocol.nft.core.model

data class TokenMeta(
    val properties: TokenProperties,
    val imageMeta: MediaMeta?
) {
    companion object {
        val EMPTY = TokenMeta(
            properties = TokenProperties.EMPTY,
            imageMeta = null,
        )
    }
}
