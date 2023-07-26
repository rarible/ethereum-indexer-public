package com.rarible.protocol.order.core.service.pool

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderAmmData
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.PoolBuyInfo
import com.rarible.protocol.order.core.model.PoolSellInfo
import com.rarible.protocol.order.core.model.orNull
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.sudoswap.SudoSwapProtocolFeeProvider
import org.springframework.stereotype.Component

@Component
class PoolPriceProvider(
    private val sudoSwapCurve: PoolCurve,
    private val sudoSwapProtocolFeeProvider: SudoSwapProtocolFeeProvider,
    private val normalizer: PriceNormalizer
) {
    suspend fun updatePoolPrice(order: Order): Order {
        return when {
            order.make.type.nft -> {
                val buyInfo = getNftBuyPrice(order, 1).orNull() ?: return order
                val take = order.take.copy(value = EthUInt256.of(buyInfo.amount))
                val makePrice = normalizer.normalize(take.type, buyInfo.amount)
                order.copy(take = take, makePrice = makePrice)
            }
            order.take.type.nft -> {
                val sellInfo = getNftSellPrice(order, 1).orNull() ?: return order
                val make = order.make.copy(value = EthUInt256.of(sellInfo.amount))
                val takePrice = normalizer.normalize(make.type, sellInfo.amount)
                order.copy(make = make, takePrice = takePrice)
            }
            else -> order
        }
    }

    suspend fun getNftBuyPrice(order: Order, nftCount: Int): PoolBuyInfo {
        val data = getAmmOrderData(order) ?: return PoolBuyInfo.ZERO
        return when (data) {
            is OrderSudoSwapAmmDataV1 -> {
                val protocolFeeMultiplier = sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(data.factory)
                val sellInfo = sudoSwapCurve.getBuyInfo(
                    curve = data.bondingCurve,
                    spotPrice = data.spotPrice,
                    delta = data.delta,
                    numItems = nftCount.toBigInteger(),
                    feeMultiplier = data.fee,
                    protocolFeeMultiplier = protocolFeeMultiplier
                )
                PoolBuyInfo(nftCount, sellInfo.inputValue)
            }
        }
    }

    suspend fun getNftSellPrice(order: Order, nftCount: Int): PoolSellInfo {
        val data = getAmmOrderData(order) ?: return PoolSellInfo.ZERO
        return when (data) {
            is OrderSudoSwapAmmDataV1 -> {
                val protocolFeeMultiplier = sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(data.factory)
                val sellInfo = sudoSwapCurve.getSellInfo(
                    curve = data.bondingCurve,
                    spotPrice = data.spotPrice,
                    delta = data.delta,
                    numItems = nftCount.toBigInteger(),
                    feeMultiplier = data.fee,
                    protocolFeeMultiplier = protocolFeeMultiplier
                )
                PoolSellInfo(nftCount, sellInfo.outputValue)
            }
        }
    }

    private fun getAmmOrderData(order: Order): OrderAmmData? {
        return when {
            order.type != OrderType.AMM -> null
            order.data !is OrderAmmData -> null
            else -> order.data
        }
    }
}
