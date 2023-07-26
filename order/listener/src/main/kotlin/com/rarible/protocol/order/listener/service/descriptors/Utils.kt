package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.protocol.order.core.misc.isSingleton
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import scalether.domain.Address

fun getOriginMaker(maker: Address, data: OrderData?): Address {
    return when (data) {
        is OrderRaribleV2DataV1 -> if (data.payouts.isSingleton) data.payouts.first().account else maker
        is OrderRaribleV2DataV2 -> if (data.payouts.isSingleton) data.payouts.first().account else maker
        is OrderRaribleV2DataV3 -> data.payout?.account ?: maker
        is OrderDataLegacy, is OrderOpenSeaV1DataV1, is OrderBasicSeaportDataV1, is OrderCryptoPunksData,
        is OrderX2Y2DataV1,
        is OrderLooksrareDataV1,
        is OrderLooksrareDataV2,
        is OrderSudoSwapAmmDataV1 -> maker
        null -> maker
    }
}
