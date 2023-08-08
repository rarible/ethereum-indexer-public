package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address

data class OwnershipId(
    val token: Address,
    val tokenId: EthUInt256,
    val owner: Address
) {
    val stringValue
        get() = "$token:$tokenId:$owner"

    val decimalStringValue
        get() = "$token:${tokenId.value}:$owner"

    override fun toString(): String {
        return stringValue
    }

    fun toItemId() = ItemId(token, tokenId)

    companion object {
        fun parseId(ownershipId: String): OwnershipId {
            return Ownership.parseId(ownershipId)
        }
    }
}
