package com.rarible.protocol.order.api.misc

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import scalether.domain.Address

fun AssetType.nftId(): String {
    fun createItemId(token: Address, tokenId: EthUInt256): String {
        return "$token:$tokenId"
    }
    return when (val asset = this) {
        is Erc721AssetType -> createItemId(asset.token, asset.tokenId)
        is Erc1155AssetType -> createItemId(asset.token, asset.tokenId)
        is CryptoPunksAssetType -> createItemId(asset.marketAddress, EthUInt256.of(asset.punkId))
        else -> throw IllegalArgumentException("Target asset $asset is not NFT")
    }
}

fun AssetType.ownershipId(owner: Address) = "${nftId()}:$owner"
