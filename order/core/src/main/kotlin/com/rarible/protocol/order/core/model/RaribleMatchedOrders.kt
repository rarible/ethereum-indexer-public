package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Word

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
        val marketplaceMarker: Word?
            get() = data.getMarketplaceMarker()

        val originFees: List<Part>?
            get() = data.getOriginFees()
    }
}
