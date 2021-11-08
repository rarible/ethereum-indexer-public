package com.rarible.protocol.erc20.core.model

import scalether.domain.Address

data class BalanceId(
    val token: Address,
    val owner: Address
) {

    val stringValue: String
        get() = "$token:$owner"

    override fun toString(): String {
        return stringValue
    }

    companion object {
        fun parseId(id: String): BalanceId {
            val parts = id.split(":")
            if (parts.size < 2) {
                throw IllegalArgumentException("Incorrect format of BalanceId: $id")
            }
            return BalanceId(Address.apply(parts[0]), Address.apply(parts[1]))
        }
    }
}
