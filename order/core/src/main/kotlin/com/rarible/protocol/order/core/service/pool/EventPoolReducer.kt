package com.rarible.protocol.order.core.service.pool

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.AmmNftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.PoolCreate
import com.rarible.protocol.order.core.model.PoolDeltaUpdate
import com.rarible.protocol.order.core.model.PoolFeeUpdate
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.PoolNftDeposit
import com.rarible.protocol.order.core.model.PoolNftIn
import com.rarible.protocol.order.core.model.PoolNftOut
import com.rarible.protocol.order.core.model.PoolNftWithdraw
import com.rarible.protocol.order.core.model.PoolSpotPriceUpdate
import org.springframework.stereotype.Component

@Component
class EventPoolReducer : Reducer<PoolHistory, Order> {
    override suspend fun reduce(entity: Order, event: PoolHistory): Order {
        return when (event) {
            is PoolCreate -> onOnChainAmmOrder(entity, event)
            is PoolNftOut -> onPoolNftOut(entity, event)
            is PoolNftIn -> onPoolNftIn(entity, event)
            is PoolSpotPriceUpdate -> onUpdateData(entity) { it.copy(spotPrice = event.newSpotPrice) }
            is PoolFeeUpdate -> onUpdateData(entity) { it.copy(fee = event.newFee) }
            is PoolDeltaUpdate -> onUpdateData(entity) { it.copy(delta = event.newDelta) }
        }
    }

    private fun onOnChainAmmOrder(entity: Order, event: PoolCreate): Order {
        return entity.copy(
            type = OrderType.AMM,
            maker = event.data.poolAddress,
            make = event.nftAsset(),
            take = event.currencyAsset(),
            makeStock = EthUInt256.of(event.tokenIds.size),
            createdAt = event.date,
            platform = event.source.toPlatform(),
            data = event.data.toOrderData(),
            id = Order.Id(event.hash),
            hash = event.hash,
        )
    }

    private fun onPoolNftOut(entity: Order, event: PoolNftOut): Order {
        if (event is PoolNftWithdraw) {
            val ammNftAssetType = when {
                entity.make.type.nft -> entity.make.type as AmmNftAssetType
                entity.take.type.nft -> entity.take.type as AmmNftAssetType
                else -> return entity
            }
            if (event.collection != ammNftAssetType.token) return entity
        }
        val newVale = entity.makeStock.subSafe(event.tokenIds.size)
        return entity.copy(makeStock = newVale)
    }

    private fun onPoolNftIn(entity: Order, event: PoolNftIn): Order {
        if (event is PoolNftDeposit) {
            val ammNftAssetType = when {
                entity.make.type.nft -> entity.make.type as AmmNftAssetType
                entity.take.type.nft -> entity.take.type as AmmNftAssetType
                else -> return entity
            }
            if (event.collection != ammNftAssetType.token) return entity
        }
        val newVale = entity.makeStock + EthUInt256.of(event.tokenIds.size)
        return entity.copy(makeStock = newVale)
    }

    private fun onUpdateData(
        entity: Order,
        update: (data: OrderSudoSwapAmmDataV1) -> OrderSudoSwapAmmDataV1
    ): Order {
        val data = when (entity.data) {
            is OrderSudoSwapAmmDataV1 -> update(entity.data)
            is OrderCryptoPunksData,
            is OrderDataLegacy,
            is OrderLooksrareDataV1,
            is OrderLooksrareDataV2,
            is OrderOpenSeaV1DataV1,
            is OrderRaribleV2DataV1,
            is OrderRaribleV2DataV2,
            is OrderRaribleV2DataV3Buy,
            is OrderRaribleV2DataV3Sell,
            is OrderBasicSeaportDataV1,
            is OrderX2Y2DataV1 -> entity.data
        }
        return entity.copy(data = data)
    }

    private fun EthUInt256.subSafe(value: Int): EthUInt256 {
        return this.subSafe(EthUInt256.of(value))
    }

    private fun EthUInt256.subSafe(value: EthUInt256): EthUInt256 {
        return if (this > value) this - value else EthUInt256.ZERO
    }
}
