package com.rarible.protocol.order.core.service.curve

import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import com.rarible.protocol.order.core.model.SudoSwapSellInfo
import scalether.domain.Address
import java.math.BigInteger

interface SudoSwapCurve {
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

    companion object {
        val WAD: BigInteger = BigInteger.TEN.pow(18)
        val MIN_PRICE: BigInteger = BigInteger.ONE // 1 gwei
    }
}
