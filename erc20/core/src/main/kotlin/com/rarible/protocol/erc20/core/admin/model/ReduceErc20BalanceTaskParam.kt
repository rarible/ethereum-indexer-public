package com.rarible.protocol.erc20.core.admin.model

import scalether.domain.Address

class ReduceErc20BalanceTaskParam(
    val token: Address
) {

    fun toParamString(): String = token.prefixed()

    companion object {

        const val ADMIN_BALANCE_REDUCE = "ADMIN_BALANCE_REDUCE"
        fun fromParamString(param: String): ReduceErc20BalanceTaskParam {
            return ReduceErc20BalanceTaskParam(Address.apply(param))
        }
    }
}