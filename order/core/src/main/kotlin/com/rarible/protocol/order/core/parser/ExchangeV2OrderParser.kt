package com.rarible.protocol.order.core.parser

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v2.rev3.ExchangeV2
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderDataVersion
import com.rarible.protocol.order.core.model.RaribleMatchedOrders
import com.rarible.protocol.order.core.model.toAssetType
import io.daonomic.rpc.domain.Binary
import scala.Tuple2
import scalether.domain.Address

object ExchangeV2OrderParser {

    fun parseMatchedOrders(input: Binary): RaribleMatchedOrders {
        return if (input.methodSignatureId() == ExchangeV2.matchOrdersSignature().id()) {
            val signature = ExchangeV2.matchOrdersSignature()
            val value = signature.`in`().decode(input, 4).value()
            RaribleMatchedOrders(
                left = RaribleMatchedOrders.SimpleOrder(
                    maker = value._1()._1(),
                    makeAssetType = value._1()._2()._1().toAssetType(),
                    takeAssetType = value._1()._4()._1().toAssetType(),
                    data = ExchangeV2OrderDataParser.parse(
                        version = Binary.apply(value._1()._8()),
                        data = Binary.apply(value._1()._9())
                    ),
                    salt = EthUInt256.of(value._1()._5())
                ),
                right = RaribleMatchedOrders.SimpleOrder(
                    maker = value._3()._1(),
                    makeAssetType = value._3()._2()._1().toAssetType(),
                    takeAssetType = value._3()._4()._1().toAssetType(),
                    data = ExchangeV2OrderDataParser.parse(
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
            val sellOrderData = ExchangeV2OrderDataParser.parse(
                version = Binary.apply(value._10()),
                data = Binary.apply(value._11())
            )
            val buyOrderData = ExchangeV2OrderDataParser.parse(
                version = if (sellOrderData.version == OrderDataVersion.RARIBLE_V2_DATA_V3_SELL) OrderDataVersion.RARIBLE_V2_DATA_V3_BUY.ethDataType!! else sellOrderData.version.ethDataType!!,
                data = Binary.apply(value._15())
            )
            val leftOrder = RaribleMatchedOrders.SimpleOrder(
                maker = value._1(),
                makeAssetType = nftAssetType,
                takeAssetType = paymentAssetType,
                data = sellOrderData,
                salt = EthUInt256.of(value._7())
            )
            val rightOrder = RaribleMatchedOrders.SimpleOrder(
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

            val buyOrderData = ExchangeV2OrderDataParser.parse(
                version = Binary.apply(value._10()),
                data = Binary.apply(value._11())
            )
            val sellOrderData = ExchangeV2OrderDataParser.parse(
                version = if (buyOrderData.version == OrderDataVersion.RARIBLE_V2_DATA_V3_BUY) OrderDataVersion.RARIBLE_V2_DATA_V3_SELL.ethDataType!! else buyOrderData.version.ethDataType!!,
                data = Binary.apply(value._15())
            )
            val bidOrder = RaribleMatchedOrders.SimpleOrder(
                maker = value._1(),
                makeAssetType = paymentAssetType,
                takeAssetType = nftAssetType,
                data = buyOrderData,
                salt = EthUInt256.of(value._7())
            )
            val sellOrder = RaribleMatchedOrders.SimpleOrder(
                maker = Address.ZERO(),
                makeAssetType = nftAssetType,
                takeAssetType = paymentAssetType,
                data = sellOrderData,
                salt = EthUInt256.ZERO
            )
            RaribleMatchedOrders(left = sellOrder, right = bidOrder)
        } else {
            throw IllegalArgumentException("Unsupported function: ${input.methodSignatureId()}")
        }
    }
}
