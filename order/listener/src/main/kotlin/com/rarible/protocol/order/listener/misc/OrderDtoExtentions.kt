package com.rarible.protocol.order.listener.misc

import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.ItemId

val OrderDto.makeNftItemId: ItemId?
    get() = getNftItemIdFromOrder(this) { make.assetType }

val OrderDto.takeNftItemId: ItemId?
    get() = getNftItemIdFromOrder(this) { take.assetType }

private fun getNftItemIdFromOrder(
    order: OrderDto,
    assetType: (order: OrderDto) -> AssetTypeDto
): ItemId? {
    return when (order) {
        is LegacyOrderDto -> getNftItemIdFromAssetType(assetType(order))
        is RaribleV2OrderDto -> getNftItemIdFromAssetType(assetType(order))
        is OpenSeaV1OrderDto -> getNftItemIdFromAssetType(assetType(order))
        is CryptoPunkOrderDto -> getNftItemIdFromAssetType(assetType(order))
    }
}

private fun getNftItemIdFromAssetType(assetType: AssetTypeDto): ItemId? {
    return when (assetType) {
        is CryptoPunksAssetTypeDto -> ItemId(assetType.contract, assetType.punkId.toBigInteger())
        is Erc1155AssetTypeDto -> ItemId(assetType.contract, assetType.tokenId)
        is Erc1155LazyAssetTypeDto -> ItemId(assetType.contract, assetType.tokenId)
        is Erc721AssetTypeDto -> ItemId(assetType.contract, assetType.tokenId)
        is Erc721LazyAssetTypeDto -> ItemId(assetType.contract, assetType.tokenId)
        is Erc20AssetTypeDto, is EthAssetTypeDto, is GenerativeArtAssetTypeDto -> null
    }
}
