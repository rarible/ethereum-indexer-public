package com.rarible.protocol.order.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.util.Hash
import java.time.Instant

@Component
@CaptureSpan(type = SpanType.APP)
class OrderReduceService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val assetMakeBalanceProvider: AssetMakeBalanceProvider,
    private val protocolCommissionProvider: ProtocolCommissionProvider,
    private val priceNormalizer: PriceNormalizer,
    private val priceUpdateService: PriceUpdateService
) {

    suspend fun updateOrder(orderHash: Word): Order? = update(orderHash = orderHash).awaitFirstOrNull()

    // TODO: current reduce implementation does not guarantee we will save the latest Order, see RPN-921.
    fun update(orderHash: Word? = null, fromOrderHash: Word? = null): Flux<Order> {
        logger.info("Update hash=$orderHash fromHash=$fromOrderHash")
        return Flux.mergeOrdered(
            compareBy { it.date },
            orderVersionRepository.findAllByHash(orderHash, fromOrderHash)
                .map { OrderUpdate.ByOrderVersion(it) },
            exchangeHistoryRepository.findLogEvents(orderHash, fromOrderHash)
                .map { OrderUpdate.ByLogEvent(it) }
        )
            .windowUntilChanged { it.orderHash }
            .concatMap { updateOrder(it) }
    }

    private sealed class OrderUpdate {
        abstract val orderHash: Word
        abstract val eventId: String
        abstract val date: Instant

        data class ByOrderVersion(val orderVersion: OrderVersion) : OrderUpdate() {
            override val orderHash get() = orderVersion.hash
            override val eventId: String get() = orderVersion.id.toHexString()
            override val date get() = orderVersion.createdAt
        }

        data class ByLogEvent(val logEvent: LogEvent) : OrderUpdate() {
            override val orderHash get() = logEvent.data.toExchangeHistory().hash
            override val eventId: String get() = logEvent.id.toHexString()
            override val date get() = logEvent.updatedAt
        }
    }

    private fun updateOrder(updates: Flux<OrderUpdate>): Mono<Order> = mono {
        // Fields used for logging only.
        var seenRevertedOnChainOrder = false
        var seenOrderHash: Word? = null

        val result = updates.asFlow().fold(emptyOrder) { order, update ->
            seenOrderHash = update?.orderHash
            when (update) {
                is OrderUpdate.ByOrderVersion -> {
                    if (update.orderVersion.onChainOrderKey != null) {
                        // On-chain order versions are processed via the OnChainOrder LogEvent-s in the next when-branch.
                        order
                    } else {
                        order.updateWith(update.orderVersion, update.eventId)
                    }
                }
                is OrderUpdate.ByLogEvent -> {
                    val exchangeHistory = update.logEvent.data.toExchangeHistory()
                    if (exchangeHistory is OnChainOrder
                        && update.logEvent.status != LogEventStatus.CONFIRMED
                        && update.logEvent.status != LogEventStatus.PENDING
                    ) {
                        seenRevertedOnChainOrder = true
                    }
                    order.updateWith(update.logEvent, exchangeHistory, update.eventId)
                }
            }
        }
        /*
        Resulting order may have EMPTY_ORDER_HASH in two cases:
        1) There were neither OrderVersion-s (for API versions) nor OnChainOrder-s (for on-chain orders) for this hash.
           => We don't have enough data to construct an order.
        2) There were some OnChainOrder-s, but they were reverted.
           => We have to remove the order from the database.
         */
        if (result.hash == EMPTY_ORDER_HASH) {
            logger.info(buildString {
                append("Order $seenOrderHash reduce ended up with empty order: ")
                append(
                    if (seenRevertedOnChainOrder) {
                        "the on-chain order was reverted"
                    } else {
                        "there were no OrderVersion-s for this hash"
                    }
                )
            })
            if (seenOrderHash != null) {
                // Remove the possibly reverted order from the OrderRepository.
                orderRepository.remove(seenOrderHash!!)
            }
            return@mono emptyOrder
        }
        updateOrderWithState(result)
    }

    private suspend fun Order.updateWith(
        logEvent: LogEvent,
        orderExchangeHistory: OrderExchangeHistory,
        eventId: String
    ): Order {
        if (orderExchangeHistory is OnChainOrder) {
            return updateWithOnChainOrder(logEvent, orderExchangeHistory, eventId)
        }
        return when (logEvent.status) {
            LogEventStatus.PENDING -> copy(pending = pending + orderExchangeHistory)
            LogEventStatus.CONFIRMED -> when (orderExchangeHistory) {
                is OrderSideMatch -> {
                    if (orderExchangeHistory.adhoc == true) {
                        /*
                         * Do not apply side matches to "virtual" orders, which are created solely for the exchange moment.
                         * This only happens to on-chain orders that have the same salt for all events.
                         * For now, this is only about CryptoPunks: their orders always have salt = 0.
                         * Consider the case:
                         * 1) Owner puts a punk on sale
                         * 2) Bidder makes a bid on the punk
                         * 3) Owner accepts the bid
                         * In this case the sell order (p. 1) must be cancelled, not filled.
                         * This is why when we're processing the OrderSideMatch for the virtual "sell" order of a bid match
                         * we must not apply it to the real sell order out there in the database.
                         */
                        this
                    } else {
                        copy(
                            fill = fill.plus(orderExchangeHistory.fill),
                            lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date),
                            lastEventId = accumulateEventId(lastEventId, eventId)
                        )
                    }
                }
                is OrderCancel -> copy(
                    cancelled = true,
                    lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date),
                    lastEventId = accumulateEventId(lastEventId, eventId)
                )
                is OnChainOrder -> error("Must have been processed above")
            }
            else -> this
        }
    }

    private suspend fun Order.updateWithOnChainOrder(
        logEvent: LogEvent,
        onChainOrder: OnChainOrder,
        eventId: String
    ): Order {
        val onChainOrderKey = logEvent.toLogEventKey()
        return if (logEvent.status == LogEventStatus.CONFIRMED || logEvent.status == LogEventStatus.PENDING) {
            val orderVersion = onChainOrder.toOrderVersion()
                .copy(onChainOrderKey = onChainOrderKey)
                .let { priceUpdateService.withUpdatedAllPrices(it) }
            if (!orderVersionRepository.existsByOnChainOrderKey(onChainOrderKey).awaitFirst()) {
                try {
                    orderVersionRepository.save(orderVersion).awaitFirst()
                } catch (ignored: DuplicateKeyException) {
                }
            }
            // On-chain orders can be re-opened, so we start from the empty state.
            emptyOrder.updateWith(orderVersion, eventId)
                .copy(pending = if (logEvent.status == LogEventStatus.PENDING) listOf(onChainOrder) else emptyList())
        } else {
            orderVersionRepository.deleteByOnChainOrderKey(onChainOrderKey).awaitFirstOrNull()
            // Skip this reverted log.
            this
        }
    }

    private suspend fun Order.updateWith(
        version: OrderVersion,
        eventId: String
    ): Order = Order(
        maker = version.maker,
        taker = version.taker,
        make = version.make,
        take = version.take,
        type = version.type,
        salt = version.salt,
        start = version.start,
        end = version.end,
        data = version.data,
        signature = version.signature,
        makePriceUsd = version.makePriceUsd,
        takePriceUsd = version.takePriceUsd,
        makePrice = version.makePrice,
        takePrice = version.takePrice,
        makeUsd = version.makeUsd,
        takeUsd = version.takeUsd,
        platform = version.platform,
        hash = version.hash,

        createdAt = createdAt.takeUnless { it == Instant.EPOCH } ?: version.createdAt,
        lastUpdateAt = version.createdAt,

        lastEventId = accumulateEventId(lastEventId, eventId),

        priceHistory = getUpdatedPriceHistoryRecords(this, version),
        fill = fill,
        cancelled = cancelled,
        makeStock = makeStock,
        pending = pending
    )

    private suspend fun getUpdatedPriceHistoryRecords(
        previous: Order,
        orderVersion: OrderVersion
    ): List<OrderPriceHistoryRecord> {
        if (previous.make == orderVersion.make && previous.take == orderVersion.take) {
            return previous.priceHistory
        }
        val newRecord = OrderPriceHistoryRecord(
            orderVersion.createdAt,
            priceNormalizer.normalize(orderVersion.make),
            priceNormalizer.normalize(orderVersion.take)
        )
        return (listOf(newRecord) + previous.priceHistory).take(Order.MAX_PRICE_HISTORIES)
    }

    private suspend fun Order.withUpdatedMakeStock(): Order {
        val makeBalance = assetMakeBalanceProvider.getMakeBalance(this)
        logger.info("Make balance $makeBalance for order $hash")
        return withMakeBalance(makeBalance, protocolCommissionProvider.get())
    }

    private suspend fun Order.withNewPrice(): Order {
        val orderUsdValue = priceUpdateService.getAssetsUsdValue(make, take, nowMillis())
        return if (orderUsdValue != null) withOrderUsdValue(orderUsdValue) else this
    }

    private suspend fun updateOrderWithState(orderStub: Order): Order {
        val order = orderStub
            .withUpdatedMakeStock()
            .withNewPrice()
        val saved = orderRepository.save(order)
        logger.info(buildString {
            append("Updated order: ")
            append("hash=${saved.hash}, ")
            append("makeStock=${saved.makeStock}, ")
            append("fill=${saved.fill}, ")
            append("cancelled=${saved.cancelled}, ")
            append("signature=${saved.signature}, ")
            append("pendingSize=${saved.pending.size},")
            append("status=${saved.status}")
        })
        return saved
    }

    companion object {
        private val EMPTY_ORDER_HASH = 0.toBigInteger().toWord()

        private val emptyOrder = Order(
            maker = Address.ZERO(),
            taker = Address.ZERO(),
            make = Asset(EthAssetType, EthUInt256.ZERO),
            take = Asset(EthAssetType, EthUInt256.ZERO),
            type = OrderType.RARIBLE_V2,
            fill = EthUInt256.ZERO,
            cancelled = false,
            makeStock = EthUInt256.ZERO,
            salt = EthUInt256.ZERO,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = Instant.EPOCH,
            lastUpdateAt = Instant.EPOCH,
            pending = emptyList(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            priceHistory = emptyList(),
            platform = Platform.RARIBLE,
            hash = EMPTY_ORDER_HASH
        )

        private fun accumulateEventId(lastEventId: String?, eventId: String): String {
            return Hash.sha3((lastEventId ?: "") + eventId)
        }

        val logger: Logger = LoggerFactory.getLogger(OrderReduceService::class.java)

        fun EventData.toExchangeHistory(): OrderExchangeHistory =
            requireNotNull(this as? OrderExchangeHistory) { "Unexpected exchange history type ${this::class}" }
    }
}
