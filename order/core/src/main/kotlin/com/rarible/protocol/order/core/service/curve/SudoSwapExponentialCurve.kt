package com.rarible.protocol.order.core.service.curve

import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import com.rarible.protocol.order.core.model.SudoSwapSellInfo
import com.rarible.protocol.order.core.service.curve.PoolCurve.Companion.MIN_PRICE
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class SudoSwapExponentialCurve : PoolCurve {
    override suspend fun getBuyInfo(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapBuyInfo {
        // NOTE: we assume delta is > 1, as checked by validateDelta()
        // We only calculate changes for buying 1 or more NFTs
        if (numItems == BigInteger.ZERO) {
            return SudoSwapBuyInfo.ZERO
        }
        if (numItems > Int.MAX_VALUE.toBigInteger()) {
            return SudoSwapBuyInfo.ZERO
        }
        val deltaPowN = delta.pow(numItems.intValueExact()) / PoolCurve.WAD.pow(numItems.intValueExact() - 1)
        // For an exponential curve, the spot price is multiplied by delta for each item bought
        // For an exponential curve, the spot price is multiplied by delta for each item bought
        val newSpotPrice = spotPrice * deltaPowN / PoolCurve.WAD
        // Spot price is assumed to be the instant sell price. To avoid arbitraging LPs, we adjust the buy price upwards.
        // If spot price for buy and sell were the same, then someone could buy 1 NFT and then sell for immediate profit.
        // EX: Let S be spot price. Then buying 1 NFT costs S ETH, now new spot price is (S * delta).
        // The same person could then sell for (S * delta) ETH, netting them delta ETH profit.
        // If spot price for buy and sell differ by delta, then buying costs (S * delta) ETH.
        // The new spot price would become (S * delta), so selling would also yield (S * delta) ETH.
        val buySpotPrice = spotPrice * delta / PoolCurve.WAD
        // If the user buys n items, then the total cost is equal to:
        // buySpotPrice + (delta * buySpotPrice) + (delta^2 * buySpotPrice) + ... (delta^(numItems - 1) * buySpotPrice)
        // This is equal to buySpotPrice * (delta^n - 1) / (delta - 1)
        val inputValue = buySpotPrice * (deltaPowN - PoolCurve.WAD) / (delta - PoolCurve.WAD)
        // Account for the protocol fee, a flat percentage of the buy amount
        val protocolFee = (inputValue * protocolFeeMultiplier) / PoolCurve.WAD
        // Account for the trade fee, only for Trade pools
        val tradeFee = (inputValue * feeMultiplier) / PoolCurve.WAD

        return SudoSwapBuyInfo(
            newSpotPrice = newSpotPrice,
            // Keep delta the same
            newDelta = delta,
            // Add the protocol fee and trade fee to the required input amount
            inputValue = inputValue + protocolFee + tradeFee,
            protocolFee = protocolFee
        )
    }

    override suspend fun getSellInfo(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapSellInfo {
        // NOTE: we assume delta is > 1, as checked by validateDelta()
        // We only calculate changes for buying 1 or more NFTs
        if (numItems == BigInteger.ZERO) {
            return SudoSwapSellInfo.ZERO
        }
        if (numItems > Int.MAX_VALUE.toBigInteger()) {
            return SudoSwapSellInfo.ZERO
        }
        val invDelta = PoolCurve.WAD.pow(2).div(delta)
        val invDeltaPowN = invDelta.pow(numItems.intValueExact()) / PoolCurve.WAD.pow(numItems.intValueExact() - 1)
        // For an exponential curve, the spot price is divided by delta for each item sold
        // safe to convert newSpotPrice directly into uint128 since we know newSpotPrice <= spotPrice
        // and spotPrice <= type(uint128).max
        val newSpotPrice = if (spotPrice * invDeltaPowN > MIN_PRICE) spotPrice * invDeltaPowN / PoolCurve.WAD else MIN_PRICE
        // If the user sells n items, then the total revenue is equal to:
        // spotPrice + ((1 / delta) * spotPrice) + ((1 / delta)^2 * spotPrice) + ... ((1 / delta)^(numItems - 1) * spotPrice)
        // This is equal to spotPrice * (1 - (1 / delta^n)) / (1 - (1 / delta))
        val outputValue = spotPrice * (PoolCurve.WAD - invDeltaPowN) / (PoolCurve.WAD - invDelta)
        // Account for the protocol fee, a flat percentage of the sell amount
        val protocolFee = outputValue * protocolFeeMultiplier / PoolCurve.WAD
        // Account for the trade fee, only for Trade pools
        val tradeFee = outputValue * feeMultiplier / PoolCurve.WAD
        return SudoSwapSellInfo(
            newSpotPrice = newSpotPrice,
            newDelta = delta,
            outputValue = outputValue - protocolFee - tradeFee,
            protocolFee = protocolFee
        )
    }
}
