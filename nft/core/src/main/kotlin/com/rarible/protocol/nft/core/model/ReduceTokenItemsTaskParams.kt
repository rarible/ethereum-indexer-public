package com.rarible.protocol.nft.core.model

import scalether.domain.Address

data class ReduceTokenItemsTaskParams(
    val token: Address
) {
    fun toParamString(): String {
        return token.prefixed()
    }

    companion object {
        const val ADMIN_REDUCE_TOKEN_ITEMS = "ADMIN_REDUCE_TOKEN_ITEMS"

        fun fromParamString(param: String): ReduceTokenItemsTaskParams {
            return ReduceTokenItemsTaskParams(Address.apply(param))
        }
    }
}
