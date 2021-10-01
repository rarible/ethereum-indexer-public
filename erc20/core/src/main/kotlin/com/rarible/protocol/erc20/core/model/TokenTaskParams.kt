package com.rarible.protocol.erc20.core.model

import scalether.domain.Address

sealed class TokenTaskParams {
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

data class ReindexTokenTransferTaskParams(override val token: Address) : TokenTaskParams() {
    companion object {
        const val ADMIN_REINDEX_TOKEN_TRANSFER = "ADMIN_REINDEX_TOKEN_TRANSFER"

        fun fromParamString(param: String): ReindexTokenTransferTaskParams {
            return ReindexTokenTransferTaskParams(
                TokenTaskParams.fromParamString(param)
            )
        }
    }
}

data class ReindexTokenWithdrawalTaskParams(override val token: Address) : TokenTaskParams() {
    companion object {
        const val ADMIN_REINDEX_TOKEN_WITHDRAWAL = "ADMIN_REINDEX_TOKEN_WITHDRAWAL"

        fun fromParamString(param: String): ReindexTokenWithdrawalTaskParams {
            return ReindexTokenWithdrawalTaskParams(
                TokenTaskParams.fromParamString(param)
            )
        }
    }
}

data class ReduceTokenTaskParams(
    override val token: Address
) : TokenTaskParams() {

    companion object {
        const val ADMIN_REDUCE_TOKEN = "ADMIN_REDUCE_TOKEN"

        fun fromParamString(param: String): ReduceTokenTaskParams {
            return ReduceTokenTaskParams(
                TokenTaskParams.fromParamString(param)
            )
        }
    }
}
