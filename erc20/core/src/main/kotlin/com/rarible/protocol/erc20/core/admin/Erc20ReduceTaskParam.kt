package com.rarible.protocol.erc20.core.admin

import com.rarible.protocol.erc20.core.model.BalanceId
import scalether.domain.Address

data class Erc20ReduceTaskParam(
    val token: Address? = null,
    val owner: Address? = null
) {

    private fun isFull() = token == null && owner == null
    private fun isByToken() = token != null && owner == null

    override fun toString(): String {
        return when {
            token == null && owner == null -> ""
            token == null -> throw IllegalArgumentException("Can't reduce balances for owner only (${owner?.hex()})")
            owner == null -> token.hex()
            else -> "${token.hex()}:${owner.hex()}"
        }
    }

    fun isOverlapped(other: Erc20ReduceTaskParam): Boolean {
        return when {
            this.isFull() || other.isFull() -> true
            this.isByToken() || other.isByToken() -> this.token == other.token
            else -> this == other
        }
    }

    companion object {

        const val TASK_TYPE = "BALANCE_REDUCE"

        fun fromString(param: String): Erc20ReduceTaskParam {
            // specific balance
            return when {
                param.contains(":") -> {
                    val balance = BalanceId.parseId(param)
                    Erc20ReduceTaskParam(balance.token, balance.owner)
                }
                // token only
                param.isNotBlank() -> Erc20ReduceTaskParam(Address.apply(param), null)
                // all balances
                else -> Erc20ReduceTaskParam(null, null)
            }
        }
    }
}
