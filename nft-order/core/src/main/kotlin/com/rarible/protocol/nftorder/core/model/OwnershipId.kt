package com.rarible.protocol.nftorder.core.model

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

    companion object {
        fun parseId(ownershipId: String): OwnershipId {
            val parts = ownershipId.split(":")
            if (parts.size < 3) {
                throw IllegalArgumentException("Incorrect format of ownershipId: $ownershipId")
            }
            val tokenId = EthUInt256.of(parts[1])
            return OwnershipId(Address.apply(parts[0]), tokenId, Address.apply(parts[2]))
        }
    }
}