package com.rarible.protocol.nft.core.model

import com.rarible.core.content.meta.loader.ContentMeta

data class TokenMeta(
    val properties: TokenProperties,
    val contentMeta: ContentMeta?
) {
    companion object {
        val EMPTY = TokenMeta(
            properties = TokenProperties.EMPTY,
            contentMeta = null,
        )
    }
}
