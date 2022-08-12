package com.rarible.protocol.order.core.service

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.zeroWord
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataVersion
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.RaribleMatchedOrders
import com.rarible.protocol.order.core.model.RaribleMatchedOrders.SimpleOrder
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.model.toPart
import com.rarible.protocol.order.core.trace.TraceCallService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import java.math.BigInteger
import com.rarible.protocol.contracts.exchange.v2.rev3.MatchEvent as MatchEventRev3

@Component
class RaribleExchangeV2OrderParser(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val traceCallService: TraceCallService,
) {
    suspend fun parseMatchedOrders(txHash: Word, txInput: Binary, event: MatchEventRev3): RaribleMatchedOrders? {
        val inputs = getInputs(txHash, txInput)
        return inputs.map { parseMatchedOrders(it) }.firstOrNull {
            Word.apply(event.leftHash()) == it.left.hash && Word.apply(event.rightHash()) == it.right.hash
        }
    }

    suspend fun parseMatchedOrders(txHash: Word, txInput: Binary, event: MatchEvent): RaribleMatchedOrders? {
        val inputs = getInputs(txHash, txInput)

        val leftAssetType = event.leftAsset().toAssetType()
        val rightAssetType = event.rightAsset().toAssetType()

        return inputs.map { parseMatchedOrders(it) }.firstOrNull { orders ->
            val leftHash = Order.hashKey(
                event.leftMaker(),
                if (orders.left.makeAssetType.isCollection) leftAssetType.tryToConvertInCollection() else leftAssetType,
                if (orders.left.takeAssetType.isCollection) rightAssetType.tryToConvertInCollection() else rightAssetType,
                orders.left.salt.value,
                orders.left.data
            )
            val rightHash = Order.hashKey(
                event.rightMaker(),
                if (orders.right.makeAssetType.isCollection) rightAssetType.tryToConvertInCollection() else rightAssetType,
                if (orders.right.takeAssetType.isCollection) leftAssetType.tryToConvertInCollection() else leftAssetType,
                orders.right.salt.value,
                orders.right.data
            )
            Word.apply(event.leftHash()) == leftHash && Word.apply(event.rightHash()) == rightHash
        }
    }

    suspend fun getInputs(txHash: Word, txInput: Binary): List<Binary> {
        return traceCallService.findAllRequiredCallInputs(
            txHash,
            txInput,
            exchangeContractAddresses.v2,
            ExchangeV2.matchOrdersSignature().id()
        )
    }

    fun parseMatchedOrders(input: Binary): RaribleMatchedOrders {
        val signature = ExchangeV2.matchOrdersSignature()
        val decoded = signature.`in`().decode(input, 4)
        return RaribleMatchedOrders(
            left = SimpleOrder(
                maker = decoded.value()._1()._1(),
                makeAssetType = decoded.value()._1()._2()._1().toAssetType(),
                takeAssetType = decoded.value()._1()._4()._1().toAssetType(),
                data = convertOrderData(
                    version = Binary.apply(decoded.value()._1()._8()),
                    data = Binary.apply(decoded.value()._1()._9())
                ),
                salt = EthUInt256.of(decoded.value()._1()._5())
            ),
            right = SimpleOrder(
                maker = decoded.value()._3()._1(),
                makeAssetType = decoded.value()._3()._2()._1().toAssetType(),
                takeAssetType = decoded.value()._3()._4()._1().toAssetType(),
                data = convertOrderData(
                    version = Binary.apply(decoded.value()._3()._8()),
                    data = Binary.apply(decoded.value()._3()._9())
                ),
                salt = EthUInt256.of(decoded.value()._3()._5())
            )
        )
    }

    fun convertOrderData(version: Binary, data: Binary): OrderData {
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

    private fun AssetType.tryToConvertInCollection(): AssetType {
        return when (this) {
            is NftAssetType -> CollectionAssetType(token)
            is CollectionAssetType,
            is Erc20AssetType,
            is EthAssetType,
            is GenerativeArtAssetType -> this
        }
    }

    private val AssetType.isCollection: Boolean
        get() = this is CollectionAssetType

    private companion object {
        val WRONG_ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX: Binary =
            Binary.apply("0x0000000000000000000000000000000000000000000000000000000000000040")
        val ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX: Binary =
            Binary.apply("0x0000000000000000000000000000000000000000000000000000000000000020")
    }
}
