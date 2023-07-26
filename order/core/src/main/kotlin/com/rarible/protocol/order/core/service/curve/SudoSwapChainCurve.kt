package com.rarible.protocol.order.core.service.curve

import com.rarible.protocol.contracts.exchange.sudoswap.v1.curve.ICurveV1
import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import com.rarible.protocol.order.core.model.SudoSwapSellInfo
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.math.BigInteger

@Component
class SudoSwapChainCurve(ethereum: MonoEthereum) : PoolCurve {
    private val sender = ReadOnlyMonoTransactionSender(ethereum, Address.ZERO())

    override suspend fun getBuyInfo(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapBuyInfo {
        val curveV1 = ICurveV1(curve, sender)
        val result = curveV1.getBuyInfo(spotPrice, delta, numItems, feeMultiplier, protocolFeeMultiplier).call().awaitFirst()
        return SudoSwapBuyInfo(
            newSpotPrice = result._2(),
            newDelta = result._3(),
            inputValue = result._4(),
            protocolFee = result._5()
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
        val curveV1 = ICurveV1(curve, sender)
        val result = curveV1.getSellInfo(spotPrice, delta, numItems, feeMultiplier, protocolFeeMultiplier).call().awaitFirst()
        return SudoSwapSellInfo(
            newSpotPrice = result._2(),
            newDelta = result._3(),
            outputValue = result._4(),
            protocolFee = result._5()
        )
    }
}
