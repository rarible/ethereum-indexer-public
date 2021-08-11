package com.rarible.protocol.nft.core.model

import java.math.BigInteger

enum class AssetType(val forContracts: BigInteger, val nft: Boolean = false) {
    ERC20(1.toBigInteger()),
    ERC1155(2.toBigInteger(), nft = true),
    ERC721(3.toBigInteger(), nft = true),
    ERC721_DEPRECATED(4.toBigInteger(), nft = true),
}