package com.rarible.protocol.order.api.data

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import scalether.domain.Address

fun createEthAsset() = Asset(
    EthAssetType,
    EthUInt256.of((1L..1000L).random())
)

fun createErc20Asset() = Asset(
    Erc20AssetType(Address.ONE()),
    EthUInt256.of((1L..1000L).random())
)

fun createErc721Asset(assetType: Erc721AssetType = createErc721AssetType() ) = Asset(
    assetType,
    EthUInt256.ONE
)

fun createErc1155Asset(assetType: Erc1155AssetType = createErc1155AssetType()) = Asset(
    assetType,
    EthUInt256.of((2L..1000L).random())
)

fun createCollectionAsset(assetType: CollectionAssetType = createCollectionAssetType() ) = Asset(
    assetType,
    EthUInt256.ONE
)
