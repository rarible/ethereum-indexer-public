package com.rarible.protocol.order.core.service.curve

import com.rarible.protocol.order.core.configuration.SudoSwapAddresses
import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import com.rarible.protocol.order.core.model.SudoSwapPurchaseValue
import com.rarible.protocol.order.core.model.SudoSwapSellInfo
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
@Primary
class CompositePoolCurve(
    private val sudoSwapLinearCurve: SudoSwapLinearCurve,
    private val sudoSwapExponentialCurve: SudoSwapExponentialCurve,
    private val sudoSwapChainCurve: SudoSwapChainCurve,
    private val sudoSwapAddresses: SudoSwapAddresses,
) : PoolCurve {

    override suspend fun getBuyInfo(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapBuyInfo {
        return getCurve(curve).getBuyInfo(
            curve = curve,
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
    }

    override suspend fun getBuyInputValues(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: Int,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): List<SudoSwapPurchaseValue> {
        return getCurve(curve).getBuyInputValues(curve, spotPrice, delta, numItems, feeMultiplier, protocolFeeMultiplier)
    }

    override suspend fun getSellOutputValues(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: Int,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): List<SudoSwapPurchaseValue> {
        return getCurve(curve).getSellOutputValues(curve, spotPrice, delta, numItems, feeMultiplier, protocolFeeMultiplier)
    }

    override suspend fun getSellInfo(
        curve: Address,
        spotPrice: BigInteger,
        delta: BigInteger,
        numItems: BigInteger,
        feeMultiplier: BigInteger,
        protocolFeeMultiplier: BigInteger
    ): SudoSwapSellInfo {
        return getCurve(curve).getSellInfo(
            curve = curve,
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
    }

    private fun getCurve(address: Address): PoolCurve {
        return when (address) {
            sudoSwapAddresses.linearCurveV1 -> sudoSwapLinearCurve
            sudoSwapAddresses.exponentialCurveV1 -> sudoSwapExponentialCurve
            else -> sudoSwapChainCurve
        }
    }
}
