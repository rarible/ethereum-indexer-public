package com.rarible.protocol.order.core.parser

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.order.core.misc.zeroWord
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataVersion
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.toPart
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import java.math.BigInteger

object ExchangeV2OrderDataParser {

    fun parse(version: Binary, data: Binary): OrderData {
        return when (version) {
            OrderDataVersion.RARIBLE_V2_DATA_V1.ethDataType -> {
                val (payouts, originFees) = when {
                    data.slice(0, 32) == ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX -> {
                        val decoded = Tuples.orderDataV1Type().decode(data, 0)
                        decoded.value()._1().map { it.toPart() } to decoded.value()._2().map { it.toPart() }
                    }
                    data.slice(0, 32) == WRONG_ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX -> {
                        val decodedWrong = Tuples.wrongOrderDataV1Type().decode(data, 0)
                        decodedWrong.value()._1().map { it.toPart() } to decodedWrong.value()._2().map { it.toPart() }
                    }
                    else -> throw IllegalArgumentException("Unsupported data encode (data=$data)")
                }
                OrderRaribleV2DataV1(
                    payouts = payouts,
                    originFees = originFees
                )
            }
            OrderDataVersion.RARIBLE_V2_DATA_V2.ethDataType -> {
                val decoded = Tuples.orderDataV2Type().decode(data, 0).value()
                val payouts = decoded._1().map { it.toPart() }
                val originFees = decoded._2().map { it.toPart() }
                val isMakeFill = decoded._3() > BigInteger.ZERO
                OrderRaribleV2DataV2(
                    payouts = payouts,
                    originFees = originFees,
                    isMakeFill = isMakeFill
                )
            }
            OrderDataVersion.RARIBLE_V2_DATA_V3_SELL.ethDataType -> {
                val decoded = Tuples.orderDataV3SellType().decode(data, 0).value()
                val payout = decoded._1().toPart()
                val originFeeFirst = decoded._2().toPart()
                val originFeeSecond = decoded._3().toPart()
                val maxFeesBasePoint = EthUInt256.of(decoded._4())
                val marketplaceMarker = decoded._5().toMarketplaceMarker()
                OrderRaribleV2DataV3Sell(
                    payout = payout,
                    originFeeFirst = originFeeFirst,
                    originFeeSecond = originFeeSecond,
                    maxFeesBasePoint = maxFeesBasePoint,
                    marketplaceMarker = marketplaceMarker
                )
            }
            OrderDataVersion.RARIBLE_V2_DATA_V3_BUY.ethDataType -> {
                val decoded = Tuples.orderDataV3BuyType().decode(data, 0).value()
                val payout = decoded._1().toPart()
                val originFeeFirst = decoded._2().toPart()
                val originFeeSecond = decoded._3().toPart()
                val marketplaceMarker = decoded._4().toMarketplaceMarker()
                OrderRaribleV2DataV3Buy(
                    payout = payout,
                    originFeeFirst = originFeeFirst,
                    originFeeSecond = originFeeSecond,
                    marketplaceMarker = marketplaceMarker
                )
            }
            else -> throw IllegalArgumentException("Unsupported order data version $version")
        }
    }

    private fun BigInteger.toPart(): Part? {
        return takeUnless { it == BigInteger.ZERO }?.let { Part.from(it) }
    }

    private fun ByteArray.toMarketplaceMarker(): Word? {
        return Word.apply(this).takeUnless { it == zeroWord() }
    }

    private val WRONG_ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX: Binary =
        Binary.apply("0x0000000000000000000000000000000000000000000000000000000000000040")

    private val ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX: Binary =
        Binary.apply("0x0000000000000000000000000000000000000000000000000000000000000020")
}
