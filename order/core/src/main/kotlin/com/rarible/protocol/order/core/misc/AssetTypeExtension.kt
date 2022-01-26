package com.rarible.protocol.order.core.misc

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import scalether.domain.Address

fun AssetType.nftId(): String {
    fun createItemId(token: Address, tokenId: EthUInt256): String {
        return "$token:$tokenId"
    }
    return when (val asset = this) {
        is Erc721AssetType -> createItemId(asset.token, asset.tokenId)
        is Erc1155AssetType -> createItemId(asset.token, asset.tokenId)
        is Erc1155LazyAssetType -> createItemId(asset.token, asset.tokenId)
        is Erc721LazyAssetType -> createItemId(asset.token, asset.tokenId)
        is CryptoPunksAssetType -> createItemId(asset.token, asset.tokenId)
        else -> throw IllegalArgumentException("Target asset $asset is not NFT")
    }
}

fun AssetType.ownershipId(owner: Address) = "${nftId()}:$owner"
