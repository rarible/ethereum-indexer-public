package com.rarible.protocol.order.listener.service.sudoswap.curve

import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import java.math.BigInteger

class SudoSwapExponentialCurve : SudoSwapCurve {
    override fun getBuyInfo(
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapBuyInfo {
        TODO()
    }
}