package com.rarible.protocol.order.listener.service.sudoswap.curve

import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import java.math.BigInteger

interface SudoSwapCurve {
    fun getBuyInfo(
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapBuyInfo

    companion object {
        val WAD: BigInteger = BigInteger.TEN.pow(18)
    }
}

