package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address

data class ItemId(
    val token: Address,
    val tokenId: EthUInt256
) {
    val stringValue: String
        get() = "$token:$tokenId"

    val decimalStringValue: String
        get() = "$token:${tokenId.value}"

    override fun toString(): String = decimalStringValue

    companion object {
        fun parseId(itemId: String): ItemId {
            return Item.parseId(itemId)
        }

        val MAX_ID: ItemId = ItemId(
            Address.apply("0xffffffffffffffffffffffffffffffffffffffff"),
            EthUInt256.of("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
        )
    }
}
