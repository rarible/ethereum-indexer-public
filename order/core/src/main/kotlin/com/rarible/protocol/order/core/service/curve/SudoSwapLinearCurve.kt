package com.rarible.protocol.order.core.service.curve

import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import com.rarible.protocol.order.core.model.SudoSwapSellInfo
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class SudoSwapLinearCurve : PoolCurve {
    override suspend fun getBuyInfo(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapBuyInfo {
        // We only calculate changes for buying 1 or more NFTs
        if (numItems == BigInteger.ZERO) {
            return SudoSwapBuyInfo.ZERO
        }
        // For a linear curve, the spot price increases by delta for each item bought
        val newSpotPrice = spotPrice + delta * numItems
        // Spot price is assumed to be the instant sell price. To avoid arbitraging LPs, we adjust the buy price upwards.
        // If spot price for buy and sell were the same, then someone could buy 1 NFT and then sell for immediate profit.
        // EX: Let S be spot price. Then buying 1 NFT costs S ETH, now new spot price is (S+delta).
        // The same person could then sell for (S+delta) ETH, netting them delta ETH profit.
        // If spot price for buy and sell differ by delta, then buying costs (S+delta) ETH.
        // The new spot price would become (S+delta), so selling would also yield (S+delta) ETH.
        val buySpotPrice = spotPrice + delta
        // If we buy n items, then the total cost is equal to:
        // (buy spot price) + (buy spot price + 1*delta) + (buy spot price + 2*delta) + ... + (buy spot price + (n-1)*delta)
        // This is equal to n*(buy spot price) + (delta)*(n*(n-1))/2
        // because we have n instances of buy spot price, and then we sum up from delta to (n-1)*delta
        val inputValue = numItems * buySpotPrice + (numItems * (numItems - BigInteger.ONE) * delta) / BigInteger.valueOf(2)
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
        // We only calculate changes for selling 1 or more NFTs
        if (numItems == BigInteger.ZERO) {
            return SudoSwapSellInfo.ZERO
        }
        // We first calculate the change in spot price after selling all of the items
        val totalPriceDecrease = delta * numItems
        // If the current spot price is less than the total amount that the spot price should change by...
        val (newSpotPrice, newNumItems) = if (spotPrice < totalPriceDecrease) {
            // We calculate how many items we can sell into the linear curve until the spot price reaches 0, rounding up
            // Then we set the new spot price to be 0. (Spot price is never negative)
            BigInteger.ZERO to (spotPrice / delta + BigInteger.ZERO)
        }
        // Otherwise, the current spot price is greater than or equal to the total amount that the spot price changes
        // Thus we don't need to calculate the maximum number of items until we reach zero spot price, so we don't modify numItems
        else {
            // The new spot price is just the change between spot price and the total price change
            (spotPrice - totalPriceDecrease) to numItems
        }
        // If we sell n items, then the total sale amount is:
        // (spot price) + (spot price - 1*delta) + (spot price - 2*delta) + ... + (spot price - (n-1)*delta)
        // This is equal to n*(spot price) - (delta)*(n*(n-1))/2
        val outputValue = (newNumItems * spotPrice) - (newNumItems * (newNumItems - BigInteger.ONE) * delta) / BigInteger.valueOf(2)
        // Account for the protocol fee, a flat percentage of the sell amount
        val protocolFee = (outputValue * protocolFeeMultiplier) / PoolCurve.WAD
        // Account for the trade fee, only for Trade pools
        val tradeFee = (outputValue * feeMultiplier) / PoolCurve.WAD
        return SudoSwapSellInfo(
            newSpotPrice = newSpotPrice,
            newDelta = delta,
            outputValue = outputValue - protocolFee - tradeFee,
            protocolFee = protocolFee
        )
    }
}
