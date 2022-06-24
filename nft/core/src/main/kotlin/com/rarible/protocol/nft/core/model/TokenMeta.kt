package com.rarible.protocol.nft.core.model

data class TokenMeta(
    val properties: TokenProperties

) {
    companion object {
        val EMPTY = TokenMeta(
            properties = TokenProperties.EMPTY
        )
    }
}
