package com.rarible.protocol.nft.core.model

import scalether.domain.Address

sealed class TokenTaskParam {
    abstract val token: Address

    fun toParamString(): String {
        return token.prefixed()
    }

    companion object {
        fun fromParamString(param: String): Address {
            return Address.apply(param)
        }
    }
}

data class ReduceTokenItemsTaskParams(override val token: Address) : TokenTaskParam() {
    companion object {
        const val ADMIN_REDUCE_TOKEN_ITEMS = "ADMIN_REDUCE_TOKEN_ITEMS"

        fun fromParamString(param: String): ReduceTokenItemsTaskParams {
            return ReduceTokenItemsTaskParams(TokenTaskParam.fromParamString(param))
        }
    }
}

data class ReindexTokenRoyaltiesTaskParam(override val token: Address) : TokenTaskParam() {
    companion object {
        const val ADMIN_REINDEX_TOKEN_ROYALTIES = "ADMIN_REINDEX_TOKEN_ROYALTIES"

        fun fromParamString(param: String): ReindexTokenRoyaltiesTaskParam {
            return ReindexTokenRoyaltiesTaskParam(TokenTaskParam.fromParamString(param))
        }
    }
}
