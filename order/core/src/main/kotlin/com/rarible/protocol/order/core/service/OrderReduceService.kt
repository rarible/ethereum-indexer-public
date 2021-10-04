package com.rarible.protocol.order.core.service

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
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.util.Hash
import java.time.Instant

@Component
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
            orderUpdateComparator,
            orderVersionRepository.findAllByHash(orderHash, fromOrderHash)
                .map { OrderUpdate.ByOrderVersion(it) },
            exchangeHistoryRepository.findLogEvents(orderHash, fromOrderHash)
                .filter { it.data !is OnChainOrder }
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
        var lastSeenUpdate: OrderUpdate? = null

        val result = updates.asFlow().fold(emptyOrder) { order, update ->
            lastSeenUpdate = update
            when (update) {
                is OrderUpdate.ByOrderVersion -> updateWith(
                    order,
                    update.orderVersion,
                    update.eventId
                )
                is OrderUpdate.ByLogEvent -> order.updateWith(
                    update.logEvent.status,
                    update.logEvent.data.toExchangeHistory(),
                    update.eventId
                )
            }
        }
        if (result.hash == EMPTY_ORDER_HASH) {
            logger.info("Order ${lastSeenUpdate?.orderHash} has not been reduced, none OrderVersion ware found")
            return@mono emptyOrder
        }
        updateOrderWithState(result)
    }

    private fun Order.updateWith(
        logEventStatus: LogEventStatus,
        orderExchangeHistory: OrderExchangeHistory,
        eventId: String
    ): Order {
        return when (logEventStatus) {
            LogEventStatus.PENDING -> copy(pending = pending + orderExchangeHistory)
            LogEventStatus.CONFIRMED -> when (orderExchangeHistory) {
                is OrderSideMatch -> copy(
                    fill = fill.plus(orderExchangeHistory.fill),
                    lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date),
                    lastEventId = accumulateEventId(lastEventId, eventId)
                )
                is OrderCancel -> copy(
                    cancelled = true,
                    lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date),
                    lastEventId = accumulateEventId(lastEventId, eventId)
                )
                is OnChainOrder -> error("OnChainOrder events must have been processed earlier.")
            }
            else -> this
        }
    }

    private suspend fun updateWith(
        accumulator: Order,
        version: OrderVersion,
        eventId: String
    ): Order {
        val previous = if (version.onChainOrderKey != null) {
            // On-chain orders can be re-opened, so we must start from the empty state again, even if there were previous events.
            emptyOrder
        } else {
            accumulator
        }

        return Order(
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
            makeUsd = version.makeUsd,
            takeUsd = version.takeUsd,
            platform = version.platform,
            hash = version.hash,

            createdAt = previous.createdAt.takeUnless { it == Instant.EPOCH } ?: version.createdAt,
            lastUpdateAt = version.createdAt,

            lastEventId = accumulateEventId(accumulator.lastEventId, eventId),

            priceHistory = getUpdatedPriceHistoryRecords(previous, version),
            fill = previous.fill,
            cancelled = previous.cancelled,
            makeStock = previous.makeStock,
            pending = previous.pending
        )
    }

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
        return withMakeBalance(makeBalance, protocolCommissionProvider.get(), zeroWhenCancelled = false)
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
            append("cancelled=${saved.cancelled}")
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

        private val wordComparator = Comparator<Word> r@{ w1, w2 ->
            val w1Bytes = w1.bytes()
            val w2Bytes = w2.bytes()
            for (i in 0 until minOf(w1Bytes.size, w2Bytes.size)) {
                if (w1Bytes[i] != w2Bytes[i]) {
                    return@r w1Bytes[i].compareTo(w2Bytes[i])
                }
            }
            return@r w1Bytes.size.compareTo(w2Bytes.size)
        }

        private fun OrderUpdate.toLogEventKey() = when (this) {
            is OrderUpdate.ByLogEvent -> logEvent.toLogEventKey()
            is OrderUpdate.ByOrderVersion -> orderVersion.onChainOrderKey
        }

        private val orderUpdateComparator: Comparator<OrderUpdate> = Comparator r@{ u1, u2 ->
            wordComparator.compare(u1.orderHash, u2.orderHash).takeUnless { it == 0 }?.let { return@r it }
            val k1 = u1.toLogEventKey()
            val k2 = u2.toLogEventKey()
            if (k1 == null || k2 == null) {
                return@r u1.date.compareTo(u2.date)
            }
            return@r k1.compareTo(k2)
        }

        val logger: Logger = LoggerFactory.getLogger(OrderReduceService::class.java)

        fun EventData.toExchangeHistory(): OrderExchangeHistory =
            requireNotNull(this as? OrderExchangeHistory) { "Unexpected exchange history type ${this::class}" }
    }
}
