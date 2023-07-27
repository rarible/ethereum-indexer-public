package com.rarible.protocol.order.core.parser

import com.rarible.protocol.contracts.exchange.seaport.v1.SeaportV1
import com.rarible.protocol.contracts.exchange.seaport.v1_4.SeaportV1_4
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.SeaportAdvancedOrder
import com.rarible.protocol.order.core.model.SeaportConsideration
import com.rarible.protocol.order.core.model.SeaportItemType
import com.rarible.protocol.order.core.model.SeaportOffer
import com.rarible.protocol.order.core.model.SeaportOrderComponents
import com.rarible.protocol.order.core.model.SeaportOrderParameters
import com.rarible.protocol.order.core.model.SeaportOrderType
import com.rarible.protocol.order.core.model.SeaportReceivedItem
import com.rarible.protocol.order.core.model.SeaportSpentItem
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scala.Tuple11
import scala.Tuple4
import scala.Tuple5
import scala.Tuple6
import scalether.domain.Address
import java.math.BigInteger

object SeaportOrderParser {
    fun parseAdvancedOrders(input: Binary): List<SeaportAdvancedOrder> {
        return when (input.methodSignatureId()) {
            // Seaport v1.1
            SeaportV1.matchAdvancedOrdersSignature().id() -> {
                val methodSignature = SeaportV1.matchAdvancedOrdersSignature()
                methodSignature.`in`().decode(input, 4).value()._1().map { advancedOrder ->
                    val params = advancedOrder._1()
                    val signature = Binary.apply(advancedOrder._4())
                    /**
                     *  address offerer; // 0x00
                     *  address zone; // 0x20
                     *  OfferItem[] offer; // 0x40
                     *  ConsiderationItem[] consideration; // 0x60
                     *  OrderType orderType; // 0x80
                     *  uint256 startTime; // 0xa0
                     *  uint256 endTime; // 0xc0
                     *  bytes32 zoneHash; // 0xe0
                     *  uint256 salt; // 0x100
                     *  bytes32 conduitKey; // 0x120
                     *  uint256 totalOriginalConsiderationItems; // 0x140
                     */
                    SeaportAdvancedOrder(
                        parameters = SeaportOrderParameters(
                            offerer = params._1(),
                            zone = params._2(),
                            offer = convertOrderOffer(params._3()),
                            consideration = convertOrderConsideration(params._4()),
                            orderType = SeaportOrderType.fromValue(params._5().intValueExact()),
                            startTime = params._6(),
                            endTime = params._7(),
                            zoneHash = Word.apply(params._8()),
                            salt = params._9(),
                            conduitKey = Word.apply(params._10()),
                            totalOriginalConsiderationItems = params._11().toLong()
                        ),
                        signature = signature
                    )
                }
            }

            // Seaport v1.4/1.5
            SeaportV1_4.matchAdvancedOrdersSignature().id() -> {
                val methodSignature = SeaportV1_4.matchAdvancedOrdersSignature()
                methodSignature.`in`().decode(input, 4).value()._1().map { advancedOrder ->
                    val params = advancedOrder._1()
                    val signature = Binary.apply(advancedOrder._4())
                    /**
                     *  address offerer; // 0x00
                     *  address zone; // 0x20
                     *  OfferItem[] offer; // 0x40
                     *  ConsiderationItem[] consideration; // 0x60
                     *  OrderType orderType; // 0x80
                     *  uint256 startTime; // 0xa0
                     *  uint256 endTime; // 0xc0
                     *  bytes32 zoneHash; // 0xe0
                     *  uint256 salt; // 0x100
                     *  bytes32 conduitKey; // 0x120
                     *  uint256 totalOriginalConsiderationItems; // 0x140
                     */
                    SeaportAdvancedOrder(
                        parameters = SeaportOrderParameters(
                            offerer = params._1(),
                            zone = params._2(),
                            offer = convertOrderOffer(params._3()),
                            consideration = convertOrderConsideration(params._4()),
                            orderType = SeaportOrderType.fromValue(params._5().intValueExact()),
                            startTime = params._6(),
                            endTime = params._7(),
                            zoneHash = Word.apply(params._8()),
                            salt = params._9(),
                            conduitKey = Word.apply(params._10()),
                            totalOriginalConsiderationItems = params._11().toLong()
                        ),
                        signature = signature
                    )
                }
            }

            else -> emptyList()
        }
    }

    fun convert(component: Tuple11<Address, Address, Array<Tuple5<BigInteger, Address, BigInteger, BigInteger, BigInteger>>, Array<Tuple6<BigInteger, Address, BigInteger, BigInteger, BigInteger, Address>>, BigInteger, BigInteger, BigInteger, ByteArray, BigInteger, ByteArray, BigInteger>): SeaportOrderComponents {
        return SeaportOrderComponents(
            offerer = component._1(),
            zone = component._2(),
            offer = convertOrderOffer(component._3()),
            consideration = convertOrderConsideration(component._4()),
            orderType = SeaportOrderType.fromValue(component._5().intValueExact()),
            startTime = component._6(),
            endTime = component._7(),
            zoneHash = Word.apply(component._8()),
            salt = component._9(),
            conduitKey = Word.apply(component._10()),
            counter = component._11()
        )
    }

    private fun convertOrderConsideration(consideration: Array<Tuple6<BigInteger, Address, BigInteger, BigInteger, BigInteger, Address>>): List<SeaportConsideration> {
        return consideration.map {
            SeaportConsideration(
                itemType = SeaportItemType.fromValue(it._1().intValueExact()),
                token = it._2(),
                identifier = it._3(),
                startAmount = it._4(),
                endAmount = it._5(),
                recipient = it._6()
            )
        }
    }

    private fun convertOrderOffer(offer: Array<Tuple5<BigInteger, Address, BigInteger, BigInteger, BigInteger>>): List<SeaportOffer> {
        return offer.map {
            SeaportOffer(
                itemType = SeaportItemType.fromValue(it._1().intValueExact()),
                token = it._2(),
                identifier = it._3(),
                startAmount = it._4(),
                endAmount = it._5()
            )
        }
    }

    fun convert(offer: Array<Tuple4<BigInteger, Address, BigInteger, BigInteger>>): List<SeaportSpentItem> {
        return offer.map {
            SeaportSpentItem(
                itemType = SeaportItemType.fromValue(it._1().intValueExact()),
                token = it._2(),
                identifier = it._3(),
                amount = it._4(),
            )
        }
    }

    fun convert(consideration: Array<Tuple5<BigInteger, Address, BigInteger, BigInteger, Address>>): List<SeaportReceivedItem> {
        return consideration.map {
            SeaportReceivedItem(
                itemType = SeaportItemType.fromValue(it._1().intValueExact()),
                token = it._2(),
                identifier = it._3(),
                amount = it._4(),
                recipient = it._5()
            )
        }
    }
}
