package com.rarible.protocol.nft.core.model

import scalether.domain.Address

data class ReindexTokenTaskParams(
    val standard: TokenStandard,
    val tokens: List<Address>
) {
    fun toParamString(): String {
        return "${standard.name}:${tokens.joinToString(",") { it.prefixed() }}"
    }

    companion object {
        const val ADMIN_REINDEX_TOKEN = "ADMIN_REINDEX_TOKEN"

        fun fromParamString(param: String): ReindexTokenTaskParams {
            val parts = param.split(":")
            require(parts.size == 2) { "Wrong param string" }

            return ReindexTokenTaskParams(
                TokenStandard.valueOf(parts[0]),
                parts[1].split(",").map { Address.apply(it) }
            )
        }
    }
}
