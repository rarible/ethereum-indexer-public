package com.rarible.protocol.nft.core.model

import scalether.domain.Address

data class ReindexTokenItemsTaskParams(
    val standard: TokenStandard,
    val tokens: List<Address>
) {
    fun toParamString(): String {
        return "${standard.name}:${tokens.joinToString(",") { it.prefixed() }}"
    }

    companion object {
        val SUPPORTED_REINDEX_TOKEN_STANDARD: Set<TokenStandard> = setOf(TokenStandard.ERC721, TokenStandard.ERC1155)
        const val ADMIN_REINDEX_TOKEN_ITEMS = "ADMIN_REINDEX_TOKEN_ITEMS"

        fun fromParamString(param: String): ReindexTokenItemsTaskParams {
            val parts = param.split(":")
            require(parts.size == 2) { "Wrong param string" }

            return ReindexTokenItemsTaskParams(
                TokenStandard.valueOf(parts[0]),
                parts[1].split(",").map { Address.apply(it) }
            )
        }
    }
}

