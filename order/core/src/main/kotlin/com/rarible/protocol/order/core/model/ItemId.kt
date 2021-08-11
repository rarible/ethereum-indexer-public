package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

class ItemId(
    val contract: Address,
    val tokenId: BigInteger
) {
    override fun toString(): String {
        return "$contract:$tokenId"
    }
}
