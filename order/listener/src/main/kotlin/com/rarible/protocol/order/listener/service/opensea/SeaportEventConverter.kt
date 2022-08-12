package com.rarible.protocol.order.listener.service.opensea

import com.rarible.ethereum.common.keccak256
import com.rarible.protocol.contracts.exchange.seaport.v1.OrderCancelledEvent
import com.rarible.protocol.contracts.exchange.seaport.v1.SeaportV1
import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.SeaportConsideration
import com.rarible.protocol.order.core.model.SeaportItemType
import com.rarible.protocol.order.core.model.SeaportOffer
import com.rarible.protocol.order.core.model.SeaportOrderComponents
import com.rarible.protocol.order.core.model.SeaportOrderType
import com.rarible.protocol.order.core.model.SeaportReceivedItem
import com.rarible.protocol.order.core.model.SeaportSpentItem
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scala.Tuple11
import scala.Tuple4
import scala.Tuple5
import scala.Tuple6
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Component
class SeaportEventConverter(
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val traceCallService: TraceCallService,
    private val featureFlags: OrderIndexerProperties.FeatureFlags
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun convert(
        event: OrderFulfilledEvent,
        date: Instant,
        input: Bytes,
    ): List<OrderSideMatch> {
        val spentItems = convert(event.offer())
        val receivedItems = convert(event.consideration())
        val make = convertSpentItems(spentItems) ?: return emptyList()
        val take = convertReceivedItems(receivedItems) ?: return emptyList()
        val maker = event.offerer()
        val taker = event.recipient().takeUnless { it == Address.ZERO() } ?: receivedItems.first().recipient
        val hash = Word.apply(event.orderHash())
        val counterHash = keccak256(hash)
        val leftUsdValue = priceUpdateService.getAssetsUsdValue(make = make, take = take, at = date)
        val rightUsdValue = priceUpdateService.getAssetsUsdValue(make = take, take = make, at = date)
        val events = listOf(
            OrderSideMatch(
                hash = hash,
                counterHash = counterHash,
                maker = maker,
                taker = taker,
                side = OrderSide.LEFT,
                make = make,
                take = take,
                fill = take.value,
                makeUsd = leftUsdValue?.makeUsd,
                takeUsd = leftUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(make),
                takeValue = prizeNormalizer.normalize(take),
                makePriceUsd = leftUsdValue?.makePriceUsd,
                takePriceUsd = leftUsdValue?.takePriceUsd,
                source = HistorySource.OPEN_SEA,
                date = date,
                adhoc = false,
                counterAdhoc = true,
                origin = null,
                originFees = null,
                externalOrderExecutedOnRarible = null
            ),
            OrderSideMatch(
                hash = counterHash,
                counterHash = hash,
                side = OrderSide.RIGHT,
                make = take,
                take = make,
                fill = make.value,
                maker = taker,
                taker = maker,
                makeUsd = rightUsdValue?.makeUsd,
                takeUsd = rightUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(take),
                takeValue = prizeNormalizer.normalize(make),
                makePriceUsd = rightUsdValue?.makePriceUsd,
                takePriceUsd = rightUsdValue?.takePriceUsd,
                date = date,
                source = HistorySource.OPEN_SEA,
                adhoc = true,
                counterAdhoc = false,
                originFees = null,
                origin = null,
                externalOrderExecutedOnRarible = null,
            )
        )
        return OrderSideMatch.addMarketplaceMarker(events, input)
    }

    suspend fun convert(
        event: OrderCancelledEvent,
        transaction: Transaction,
        index: Int,
        totalLogs: Int,
        date: Instant
    ): List<OrderCancel> {
        val inputs = if (CANCEL_SIGNATURE_ID == transaction.input().methodSignatureId()) {
            listOf(transaction.input())
        } else {
            if (featureFlags.skipGetTrace) {
               emptyList()
            } else {
                traceCallService.findAllRequiredCallInputs(
                    txHash = transaction.hash(),
                    txInput = transaction.input(),
                    to = event.log().address(),
                    ids = arrayOf(CANCEL_SIGNATURE_ID)
                )
            }
        }
        val orderHash = Word.apply(
            event.orderHash()
        )
        val assets = if (inputs.isNotEmpty()) {
            val components = inputs.map { CANCEL_SIGNATURE.`in`().decode(it, 4).value().toList() }.flatten()
            require(components.size == totalLogs) {
                "Order components size (${components.size}) is net equals totalLogs=${totalLogs} (tx=${transaction.hash()})"
            }

            val targetComponent = convert(components[index])
            require(Order.seaportV1Hash(targetComponent) == orderHash) {
                "Components hash needn't match order hash $orderHash in  (tx=${transaction.hash()})"
            }
            convertToAsserts(targetComponent)
        } else {
            null
        }
        return listOf(
            OrderCancel(
                hash = orderHash,
                maker = event.offerer(),
                make = assets?.make,
                take = assets?.take,
                date = date,
                source = HistorySource.OPEN_SEA
            )
        )
    }

    private fun convert(component: Tuple11<Address, Address, Array<Tuple5<BigInteger, Address, BigInteger, BigInteger, BigInteger>>, Array<Tuple6<BigInteger, Address, BigInteger, BigInteger, BigInteger, Address>>, BigInteger, BigInteger, BigInteger, ByteArray, BigInteger, ByteArray, BigInteger>): SeaportOrderComponents {
        return SeaportOrderComponents(
            offerer = component._1(),
            zone = component._2(),
            offer = convertOrderOffer(component._3()),
            consideration = convertOrderConsideration(component._4()),
            orderType = SeaportOrderType.fromValue(component._5().intValueExact()) ,
            startTime = component._6().toLong(),
            endTime = component._7().toLong(),
            zoneHash = Word.apply(component._8()),
            salt = component._9(),
            conduitKey = Word.apply(component._10()),
            counter = component._11().toLong()
        )
    }

    private fun convertToAsserts(components: SeaportOrderComponents): OrderAssets? {
        val offer = components.offer
        val consideration = components.consideration

        if (offer.size != 1) return null
        if (consideration.isEmpty()) return null

        val offererConsideration = consideration.first()
        val totalConsiderationAmount = consideration.filter { it.itemType == offererConsideration.itemType }.sumOf { it.startAmount }

        val make = offer.single().takeIf { it.isSupportedItem() } ?: return null
        val take = offererConsideration.withStartAmount(totalConsiderationAmount).takeIf { it.isSupportedItem() } ?: return null
        return OrderAssets(make.toAssetWithStartAmount(), take.toAssetWithStartAmount())
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

    private fun convert(offer: Array<Tuple4<BigInteger, Address, BigInteger, BigInteger>>): List<SeaportSpentItem> {
        return offer.map {
            SeaportSpentItem(
                itemType = SeaportItemType.fromValue(it._1().intValueExact()),
                token = it._2(),
                identifier = it._3(),
                amount = it._4(),
            )
        }
    }

    private fun convert(consideration: Array<Tuple5<BigInteger, Address, BigInteger, BigInteger, Address>>): List<SeaportReceivedItem> {
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

    private fun convertSpentItems(spentItems: List<SeaportSpentItem>): Asset? {
        if (spentItems.size != 1) {
            logger.warn("Can't convert span item with more then one element ({})", spentItems)
            return null
        }
        return spentItems.single().toAsset()
    }

    private fun convertReceivedItems(receivedItems: List<SeaportReceivedItem>): Asset? {
        if (receivedItems.isEmpty()) {
            logger.warn("No elements in received items")
            return null
        }
        val offererItem = receivedItems.first()
        val items = receivedItems.filter { it.itemType == offererItem.itemType }
        if (items.map { it.toAsset().type }.toSet().size != 1) {
            logger.warn("Offerer type has different assert types ({})", receivedItems)
            return null
        }
        val totalValue = items.fold(BigInteger.ZERO) { acc, next -> acc + next.amount }
        return offererItem.withAmount(totalValue).toAsset()
    }

    private data class OrderAssets(val make: Asset, val take: Asset)

    private companion object {
        @Suppress("HasPlatformType")
        val CANCEL_SIGNATURE = SeaportV1.cancelSignature()
        @Suppress("HasPlatformType")
        val CANCEL_SIGNATURE_ID = CANCEL_SIGNATURE.id()
    }
}