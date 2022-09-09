package com.rarible.protocol.order.listener.service.sudoswap.curve

import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import java.math.BigInteger

class SudoSwapLinearCurve : SudoSwapCurve {
    override fun getBuyInfo(
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
        val newSpotPrice = spotPrice + delta * numItems;

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
        val inputValue = numItems * buySpotPrice + (numItems * (numItems - BigInteger.ONE) * delta) / BigInteger.valueOf(2);

        // Account for the protocol fee, a flat percentage of the buy amount
        val protocolFee = (inputValue * protocolFeeMultiplier) / SudoSwapCurve.WAD

        // Account for the trade fee, only for Trade pools
        val tradeFee = (inputValue * feeMultiplier) / SudoSwapCurve.WAD

        return SudoSwapBuyInfo(
            newSpotPrice = newSpotPrice,
            // Keep delta the same
            newDelta = delta,
            // Add the protocol fee and trade fee to the required input amount
            inputValue = inputValue + protocolFee + tradeFee,
            protocolFee = protocolFee
        )
    }
}

