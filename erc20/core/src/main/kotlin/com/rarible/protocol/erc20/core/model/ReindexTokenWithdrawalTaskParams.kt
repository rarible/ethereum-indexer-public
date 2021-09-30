package com.rarible.protocol.erc20.core.model

import scalether.domain.Address

data class ReindexTokenWithdrawalTaskParams(
    val token: Address
) {
    fun toParamString(): String {
        return token.prefixed()
    }

    companion object {
        const val ADMIN_REINDEX_TOKEN_WITHDRAWAL = "ADMIN_REINDEX_TOKEN_WITHDRAWAL"

        fun fromParamString(param: String): ReindexTokenWithdrawalTaskParams {
            return ReindexTokenWithdrawalTaskParams(Address.apply(param))
        }
    }
}
