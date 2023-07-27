package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.common.keccak256
import com.rarible.protocol.contracts.exchange.seaport.v1.OrderCancelledEvent
import com.rarible.protocol.contracts.exchange.seaport.v1.SeaportV1
import com.rarible.protocol.contracts.exchange.seaport.v1_4.SeaportV1_4
import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.SeaportOrderComponents
import com.rarible.protocol.order.core.model.SeaportOrderParameters
import com.rarible.protocol.order.core.model.SeaportReceivedItem
import com.rarible.protocol.order.core.model.SeaportSpentItem
import com.rarible.protocol.order.core.parser.SeaportOrderParser
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.converter.AbstractEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Component
class SeaportEventConverter(
    traceCallService: TraceCallService,
    featureFlags: OrderIndexerProperties.FeatureFlags,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val wrapperSeaportMatchEventMetric: RegisteredCounter,
    private val nonceHistoryRepository: NonceHistoryRepository,
) : AbstractEventConverter(traceCallService, featureFlags) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun convert(
        event: OrderFulfilledEvent,
        date: Instant,
        input: Bytes,
    ): List<OrderSideMatch> {
        val spentItems = SeaportOrderParser.convert(event.offer())
        val receivedItems = SeaportOrderParser.convert(event.consideration())
        logger.info(
            "Event ${event.log().logIndex()}" +
                " in tx=${event.log().transactionHash()}" +
                " for order ${Word.apply(event.orderHash())}" +
                " contains spentItems: $spentItems and receivedItems: $receivedItems"
        )
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
                externalOrderExecutedOnRarible = null,
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
        return OrderSideMatch.addMarketplaceMarker(events, input, wrapperSeaportMatchEventMetric)
    }

    suspend fun isAdhocOrderEvent(
        event: OrderFulfilledEvent,
        index: Int,
        totalLogs: Int,
        transaction: Transaction
    ): Boolean {
        val txHash = transaction.hash()
        val logIndex = event.log().logIndex()
        val protocol = event.log().address()
        val hash = Word.apply(event.orderHash())

        val advancedOrders = getMethodInput(
            event.log(),
            transaction,
            getTrace = false,
            MATCH_ADVANCED_ORDERS_SIGNATURE_ID_V1,
            MATCH_ADVANCED_ORDERS_SIGNATURE_ID_V1_4,
        ).map { SeaportOrderParser.parseAdvancedOrders(it) }.flatten()

        return if (advancedOrders.size == totalLogs) {
            val advancedOrder = advancedOrders[index]
            if (canFindAdvancedOrderCounter(advancedOrder.parameters, hash, protocol)) {
                advancedOrder.signature == Binary.empty()
            } else {
                logger.warn("Can't find counter to match hash, tx=$txHash, logIndex=$logIndex, protocol=$protocol, parameters=${advancedOrder.parameters}")
                false
            }
        } else {
            var found = false
            for (advancedOrder in advancedOrders) {
                val isTargetOrder = canFindAdvancedOrderCounter(advancedOrder.parameters, hash, protocol)
                found = isTargetOrder && advancedOrder.signature == Binary.empty()
                if (found) break
            }
            logger.info("Search order hash result $found, tx=$txHash, logIndex=$logIndex, totalLogs=$totalLogs, index=$index")
            found
        }
    }

    private suspend fun canFindAdvancedOrderCounter(
        parameters: SeaportOrderParameters,
        expectedHash: Word,
        protocol: Address
    ): Boolean {
        val maxCounter = getMaxCounter(parameters.offerer, protocol)
        val max = if (maxCounter < BigInteger.valueOf(Long.MAX_VALUE)) {
            maxCounter.toLong()
        } else {
            0L // TODO Deal with super big counter!
        }
        for (counter in max downTo 0L) { // TODO ???
            if (Order.seaportV1Hash(parameters.normalize(), counter.toBigInteger()) == expectedHash) return true
        }
        return false
    }

    private suspend fun getMaxCounter(maker: Address, contract: Address): BigInteger {
        val log = nonceHistoryRepository.findLatestNonceHistoryByMaker(maker, contract)
        return (log?.data as? ChangeNonceHistory)?.newNonce?.value ?: BigInteger.ZERO
    }

    suspend fun convert(
        event: OrderCancelledEvent,
        transaction: Transaction,
        index: Int,
        totalLogs: Int,
        date: Instant
    ): List<OrderCancel> {
        val inputs = getMethodInput(event.log(), transaction, getTrace = true, CANCEL_SIGNATURE_ID)
        val orderHash = Word.apply(event.orderHash())
        logger.info("Event to cancel order $orderHash, tx=${transaction.hash()}, found inputs=$inputs")
        val assets = if (inputs.isNotEmpty()) {
            try {
                val components = inputs.map { CANCEL_SIGNATURE.`in`().decode(it, 4).value().toList() }.flatten()
                require(components.size == totalLogs) {
                    "Order components size (${components.size}) is net equals totalLogs=$totalLogs (tx=${transaction.hash()})"
                }
                val targetComponent = SeaportOrderParser.convert(components[index])
                require(Order.seaportV1Hash(targetComponent) == orderHash) {
                    "Components hash needn't match order hash $orderHash in  (tx=${transaction.hash()})"
                }
                convertToAsserts(targetComponent)
            } catch (ex: Throwable) {
                logger.error("Can't parse cancel orders input, tx=${transaction.hash()}", ex)
                null
            }
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

    private fun convertToAsserts(components: SeaportOrderComponents): OrderAssets? {
        val offer = components.offer
        val consideration = components.consideration

        if (offer.size != 1) return null
        if (consideration.isEmpty()) return null

        val offererConsideration = consideration.first()
        val totalConsiderationAmount =
            consideration.filter { it.itemType == offererConsideration.itemType }.sumOf { it.startAmount }

        val make = offer.single().takeIf { it.isSupportedItem() } ?: return null
        val take = offererConsideration.withStartAmount(totalConsiderationAmount).takeIf { it.isSupportedItem() }
            ?: return null
        return OrderAssets(make.toAssetWithStartAmount(), take.toAssetWithStartAmount())
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

    suspend fun getMethodInput(
        log: Log,
        transaction: Transaction,
        getTrace: Boolean,
        vararg methodId: Binary
    ): List<Binary> {
        return if (transaction.input().methodSignatureId() in methodId) {
            listOf(transaction.input())
        } else if (getTrace) {
            traceCallService.safeFindAllRequiredCallInputs(
                txHash = transaction.hash(),
                txInput = transaction.input(),
                to = log.address(),
                ids = methodId
            )
        } else emptyList()
    }

    private data class OrderAssets(val make: Asset, val take: Asset)

    private companion object {

        @Suppress("HasPlatformType")
        val CANCEL_SIGNATURE = SeaportV1.cancelSignature()

        @Suppress("HasPlatformType")
        val CANCEL_SIGNATURE_ID = CANCEL_SIGNATURE.id()

        @Suppress("HasPlatformType")
        val MATCH_ADVANCED_ORDERS_SIGNATURE_V1 = SeaportV1.matchAdvancedOrdersSignature()

        @Suppress("HasPlatformType")
        val MATCH_ADVANCED_ORDERS_SIGNATURE_V1_4 = SeaportV1_4.matchAdvancedOrdersSignature()

        @Suppress("HasPlatformType")
        val MATCH_ADVANCED_ORDERS_SIGNATURE_ID_V1 = MATCH_ADVANCED_ORDERS_SIGNATURE_V1.id()

        @Suppress("HasPlatformType")
        val MATCH_ADVANCED_ORDERS_SIGNATURE_ID_V1_4 = MATCH_ADVANCED_ORDERS_SIGNATURE_V1_4.id()
    }
}
