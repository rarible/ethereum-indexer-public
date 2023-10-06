package com.rarible.protocol.nft.core.model

import io.daonomic.rpc.domain.Binary
import scalether.util.Hash

enum class TokenFeature(val erc165: List<Binary> = emptyList()) {
    APPROVE_FOR_ALL(listOf(Binary.apply("0x80ac58cd"), Binary.apply("0xd9b67a26"))),
    SET_URI_PREFIX,
    BURN,
    MINT_WITH_ADDRESS(listOf(Binary.apply(Hash.sha3("MINT_WITH_ADDRESS".toByteArray(Charsets.US_ASCII))).slice(0, 4))),
    SECONDARY_SALE_FEES(listOf(Binary.apply("0xb7799584"))),
    MINT_AND_TRANSFER(listOf(Binary.apply("0x8486f69f"), Binary.apply("0x6db15a0f"))),
    NOT_FOR_SALE(listOf(
        Binary.apply("0x0489b56f"), // eip-5484
        Binary.apply("0x0e89341c"), // eip-5516
        Binary.apply("0x911ec470")) // eip-5633
    ),
}
