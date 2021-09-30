package com.rarible.protocol.erc20.core.model

import scalether.domain.Address

data class ReindexTokenTransferTaskParams(
    val token: Address
) {
    fun toParamString(): String {
        return token.prefixed()
    }

    companion object {
        const val ADMIN_REINDEX_TOKEN_TRANSFER = "ADMIN_REINDEX_TOKEN_TRANSFER"

        fun fromParamString(param: String): ReindexTokenTransferTaskParams {
            return ReindexTokenTransferTaskParams(Address.apply(param))
        }
    }
}
