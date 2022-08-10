package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

data class RaribleMatchedOrders(
    val left: SimpleOrder,
    val right: SimpleOrder
) {
    data class SimpleOrder(
        val maker: Address,
        val makeAssetType: AssetType,
        val takeAssetType: AssetType,
        val data: OrderData,
        val salt: EthUInt256
    ) {
        val isMakeFillOrder: Boolean
            get() = when (data) {
                is OrderRaribleV2DataV3Sell -> true
                is OrderRaribleV2DataV3Buy -> false
                else -> data.isMakeFillOrder(makeAssetType.nft)
            }

        val hash: Word
            get() = Order.hashKey(
                maker = maker,
                makeAssetType = makeAssetType,
                takeAssetType = takeAssetType,
                salt = salt.value,
                data = data
            )

        val marketplaceMarker: Word?
            get() = data.getMarketplaceMarker()

        val originFees: List<Part>?
            get() = data.getOriginFees()
    }
}
