package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256

data class RaribleMatchedOrders(
    val left: SimpleOrder,
    val right: SimpleOrder
) {
    data class SimpleOrder(
        val makeAssetType: AssetType,
        val takeAssetType: AssetType,
        val data: OrderData,
        val salt: EthUInt256
    ) {
        val originFees: List<Part>?
            get() {
                return when (data) {
                    is OrderRaribleV2DataV1 -> data.originFees
                    is OrderRaribleV2DataV2 -> data.originFees
                    is OrderOpenSeaV1DataV1,
                    is OrderBasicSeaportDataV1,
                    is OrderCryptoPunksData,
                    is OrderDataLegacy -> null
                }
            }
    }
}
