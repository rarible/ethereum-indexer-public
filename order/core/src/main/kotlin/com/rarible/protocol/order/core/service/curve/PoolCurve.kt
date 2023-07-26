package com.rarible.protocol.order.core.service.curve

import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import com.rarible.protocol.order.core.model.SudoSwapPurchaseValue
import com.rarible.protocol.order.core.model.SudoSwapSellInfo
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

interface PoolCurve {

    suspend fun getSellInfo(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapSellInfo

    suspend fun getBuyInfo(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapBuyInfo

    suspend fun getBuyInputValues(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: Int,
        feeMultiplier: BigInteger = BigInteger.ZERO,
        protocolFeeMultiplier: BigInteger = BigInteger.ZERO
    ): List<SudoSwapPurchaseValue> {
        val values = mutableListOf<SudoSwapPurchaseValue>()
        var lastPurchaseValue = SudoSwapPurchaseValue(
            newSpotPrice = spotPrice,
            newDelta = delta,
            value = BigInteger.ZERO
        )
        for (index in (1..numItems)) {
            val sudoSwapBuyInfo = getBuyInfo(
                curve = curve,
                spotPrice = lastPurchaseValue.newSpotPrice,
                delta = lastPurchaseValue.newDelta,
                numItems = BigInteger.ONE,
                feeMultiplier = feeMultiplier,
                protocolFeeMultiplier = protocolFeeMultiplier
            )
            lastPurchaseValue = SudoSwapPurchaseValue(
                newSpotPrice = sudoSwapBuyInfo.newSpotPrice,
                newDelta = sudoSwapBuyInfo.newDelta,
                value = sudoSwapBuyInfo.inputValue,
            )
            values.add(lastPurchaseValue)
        }
        return values.toList()
    }

    suspend fun getSellOutputValues(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: Int,
        feeMultiplier: BigInteger = BigInteger.ZERO,
        protocolFeeMultiplier: BigInteger = BigInteger.ZERO
    ): List<SudoSwapPurchaseValue> {
        val values = mutableListOf<SudoSwapPurchaseValue>()
        var lastPurchaseValue = SudoSwapPurchaseValue(
            newSpotPrice = spotPrice,
            newDelta = delta,
            value = BigInteger.ZERO
        )
        for (index in (1..numItems)) {
            val sudoSwapBuyInfo = getSellInfo(
                curve = curve,
                spotPrice = lastPurchaseValue.newSpotPrice,
                delta = lastPurchaseValue.newDelta,
                numItems = BigInteger.ONE,
                feeMultiplier = feeMultiplier,
                protocolFeeMultiplier = protocolFeeMultiplier
            )
            lastPurchaseValue = SudoSwapPurchaseValue(
                newSpotPrice = sudoSwapBuyInfo.newSpotPrice,
                newDelta = sudoSwapBuyInfo.newDelta,
                value = sudoSwapBuyInfo.outputValue,
            )
            values.add(lastPurchaseValue)
        }
        return values.toList()
    }

    companion object {
        fun BigInteger.eth(): BigInteger = this.multiply(WAD)
        fun BigDecimal.eth(): BigInteger = this.multiply(WAD.toBigDecimal()).toBigInteger()

        val WAD: BigInteger = BigInteger.TEN.pow(18)
        val MIN_PRICE: BigInteger = BigInteger.ONE // 1 gwei
    }
}
