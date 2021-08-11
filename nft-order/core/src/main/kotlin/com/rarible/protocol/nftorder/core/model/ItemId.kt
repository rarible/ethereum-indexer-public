package com.rarible.protocol.nftorder.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.math.BigInteger

data class ItemId(
    val token: Address,
    val tokenId: EthUInt256
) {
    val stringValue: String
        get() = "$token:$tokenId"

    val decimalStringValue: String
        get() = "$token:${tokenId.value}"

    override fun toString(): String {
        return stringValue
    }

    companion object {
        fun parseId(itemId: String): ItemId {
            val parts = itemId.split(":")
            if (parts.size < 2) {
                throw IllegalArgumentException("Incorrect format of itemId: $itemId")
            }
            val tokenId = EthUInt256.of(parts[1])
            return ItemId(Address.apply(parts[0]), tokenId)
        }

        fun of(contract: Address, tokenId: BigInteger): ItemId {
            return ItemId(contract, EthUInt256.Companion.of(tokenId))
        }
    }
}