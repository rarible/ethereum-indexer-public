package com.rarible.protocol.nft.core.model

import com.rarible.protocol.nft.core.model.meta.EthMetaContent

data class TokenMeta(
    val properties: TokenProperties,
    val contentMeta: ContentMeta?,
    val content: List<EthMetaContent> = emptyList()

) {
    companion object {
        val EMPTY = TokenMeta(
            properties = TokenProperties.EMPTY,
            contentMeta = null,
        )
    }
}
