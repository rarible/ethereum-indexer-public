package com.rarible.protocol.order.core.service

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.contracts.exchange.v2.rev3.ExchangeV2
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.misc.zeroWord
import com.rarible.protocol.order.core.model.AmmNftAssetType
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
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.model.toPart
import com.rarible.protocol.order.core.trace.TraceCallService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scala.Tuple2
import scalether.domain.Address
import java.math.BigInteger
import com.rarible.protocol.contracts.exchange.v2.rev3.MatchEvent as MatchEventRev3

@Component
class RaribleExchangeV2OrderParser(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val traceCallService: TraceCallService,
) {
    suspend fun parseMatchedOrders(txHash: Word, txInput: Binary, event: MatchEventRev3): RaribleMatchedOrders? {
        val inputs = getInputs(txHash, txInput)
        val parsed = inputs.map { parseMatchedOrders(it) }
        logger.info("Hash: $txHash; Event: $event; Parsed Matched Orders: ${parsed.map { Triple(it, it.left.hash, it.right.hash) }}")
        return parsed.firstOrNull {
            Word.apply(event.leftHash()) == it.left.hash && Word.apply(event.rightHash()) == it.right.hash
        }
    }

    suspend fun parseMatchedOrders(txHash: Word, txInput: Binary, event: MatchEvent): RaribleMatchedOrders? {
        val inputs = getInputs(txHash, txInput)

        val leftAssetType = event.leftAsset().toAssetType()
        val rightAssetType = event.rightAsset().toAssetType()

        val parsed = inputs.map { parseMatchedOrders(it) }
        logger.info("Hash: $txHash; Event: $event; Parsed Matched Orders: ${parsed.map { Triple(it, it.left.hash, it.right.hash) }}")
        return parsed.firstOrNull { orders ->
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
            ExchangeV2.matchOrdersSignature().id(),
            ExchangeV2.directPurchaseSignature().id(),
            ExchangeV2.directAcceptBidSignature().id(),
        )
    }

    fun parseMatchedOrders(input: Binary): RaribleMatchedOrders {
        return if (input.methodSignatureId() == ExchangeV2.matchOrdersSignature().id()) {
            val signature = ExchangeV2.matchOrdersSignature()
            val value = signature.`in`().decode(input, 4).value()
            RaribleMatchedOrders(
                left = SimpleOrder(
                    maker = value._1()._1(),
                    makeAssetType = value._1()._2()._1().toAssetType(),
                    takeAssetType = value._1()._4()._1().toAssetType(),
                    data = convertOrderData(
                        version = Binary.apply(value._1()._8()),
                        data = Binary.apply(value._1()._9())
                    ),
                    salt = EthUInt256.of(value._1()._5())
                ),
                right = SimpleOrder(
                    maker = value._3()._1(),
                    makeAssetType = value._3()._2()._1().toAssetType(),
                    takeAssetType = value._3()._4()._1().toAssetType(),
                    data = convertOrderData(
                        version = Binary.apply(value._3()._8()),
                        data = Binary.apply(value._3()._9())
                    ),
                    salt = EthUInt256.of(value._3()._5())
                )
            )
        } else if (input.methodSignatureId() == ExchangeV2.directPurchaseSignature().id()) {
            /*struct Purchase {
                1 address sellOrderMaker;
                2 uint256 sellOrderNftAmount;
                3 bytes4 nftAssetClass;
                4 bytes nftData;
                5 uint256 sellOrderPaymentAmount;
                6 address paymentToken;
                7 uint256 sellOrderSalt;
                8 uint sellOrderStart;
                9 uint sellOrderEnd;
                10 bytes4 sellOrderDataType;
                11 bytes sellOrderData;
                12 bytes sellOrderSignature;

                13 uint256 buyOrderPaymentAmount;
                14 uint256 buyOrderNftAmount;
                15 bytes buyOrderData;
            }*/
            val signature = ExchangeV2.directPurchaseSignature()
            val value = signature.`in`().decode(input, 4).value()
            val paymentToken = value._6()
            val nftAssetType = Tuple2(value._3(), value._4()).toAssetType()
            val paymentAssetType = if (paymentToken == Address.ZERO()) EthAssetType else Erc20AssetType(paymentToken)
            val sellOrderData = convertOrderData(
                version = Binary.apply(value._10()),
                data = Binary.apply(value._11())
            )
            val buyOrderData = convertOrderData(
                version = if (sellOrderData.version == OrderDataVersion.RARIBLE_V2_DATA_V3_SELL) OrderDataVersion.RARIBLE_V2_DATA_V3_BUY.ethDataType!! else sellOrderData.version.ethDataType!!,
                data = Binary.apply(value._15())
            )
            val leftOrder = SimpleOrder(
                maker = value._1(),
                makeAssetType = nftAssetType,
                takeAssetType = paymentAssetType,
                data = sellOrderData,
                salt = EthUInt256.of(value._7())
            )
            val rightOrder = SimpleOrder(
                maker = Address.ZERO(),
                makeAssetType = paymentAssetType,
                takeAssetType = nftAssetType,
                data = buyOrderData,
                salt = EthUInt256.ZERO
            )
            RaribleMatchedOrders(left = leftOrder, right = rightOrder)
        } else if (input.methodSignatureId() == ExchangeV2.directAcceptBidSignature().id()) {
            /*
            struct AcceptBid {
            1. address bidMaker; //
            2. uint256 bidNftAmount;
            3. bytes4 nftAssetClass;
            4. bytes nftData;
            5. uint256 bidPaymentAmount;
            6. address paymentToken;
            7. uint256 bidSalt;
            8. uint bidStart;
            9. uint bidEnd;
            10. bytes4 bidDataType;
            11. bytes bidData;
            12. bytes bidSignature;

            13. uint256 sellOrderPaymentAmount;
            14. uint256 sellOrderNftAmount;
            15. bytes sellOrderData;
            }*/

            val signature = ExchangeV2.directAcceptBidSignature()
            val value = signature.`in`().decode(input, 4).value()
            val paymentToken = value._6()
            val nftAssetType = Tuple2(value._3(), value._4()).toAssetType()
            val paymentAssetType = if (paymentToken == Address.ZERO()) EthAssetType else Erc20AssetType(paymentToken)

            val buyOrderData = convertOrderData(
                version = Binary.apply(value._10()),
                data = Binary.apply(value._11())
            )
            val sellOrderData = convertOrderData(
                version = if (buyOrderData.version == OrderDataVersion.RARIBLE_V2_DATA_V3_BUY) OrderDataVersion.RARIBLE_V2_DATA_V3_SELL.ethDataType!! else buyOrderData.version.ethDataType!!,
                data = Binary.apply(value._15())
            )
            val leftOrder = SimpleOrder(
                maker = value._1(),
                makeAssetType = paymentAssetType,
                takeAssetType = nftAssetType,
                data = buyOrderData,
                salt = EthUInt256.of(value._7())
            )
            val rightOrder = SimpleOrder(
                maker = Address.ZERO(),
                makeAssetType = nftAssetType,
                takeAssetType = paymentAssetType,
                data = sellOrderData,
                salt = EthUInt256.ZERO
            )
            RaribleMatchedOrders(left = leftOrder, right = rightOrder)
        } else {
            throw IllegalArgumentException("Unsupported function: ${input.methodSignatureId()}")
        }
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
            is AmmNftAssetType -> CollectionAssetType(token)
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
