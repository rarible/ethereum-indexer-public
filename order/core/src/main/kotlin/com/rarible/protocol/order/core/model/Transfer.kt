package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import scalether.domain.Address
import java.math.BigInteger

data class Transfer(
    val type: Type,
    val from: Address,
    val to: Address,
    val tokenId: BigInteger,
    val value: BigInteger,
    val data: Binary
) {
    enum class Type {
        ERC721,
        ERC1155
    }
}
