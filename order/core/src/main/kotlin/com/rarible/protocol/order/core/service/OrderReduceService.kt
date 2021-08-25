package com.rarible.protocol.order.core.service

import com.rarible.core.common.fromOptional
import com.rarible.core.common.nowMillis
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Component
class OrderReduceService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val assetBalanceProvider: AssetBalanceProvider,
    private val protocolCommissionProvider: ProtocolCommissionProvider,
    private val priceNormalizer: PriceNormalizer,
    private val priceUpdateService: PriceUpdateService
) {

    suspend fun updateOrderMakeStock(orderHash: Word, knownMakeBalance: EthUInt256? = null): Order {
        val order = update(orderHash = orderHash).awaitSingle()
        val withMakeStock = order.withUpdatedMakeStock(knownMakeBalance)
        val updated = if (order.makeStock == EthUInt256.ZERO && withMakeStock.makeStock != EthUInt256.ZERO) {
            priceUpdateService.updateOrderPrice(withMakeStock, Instant.now())
        } else {
            withMakeStock
        }
        val saved = orderRepository.save(updated)
        logger.info("Updated order $orderHash, makeStock=${saved.makeStock}")
        return saved
    }

    fun update(
        orderHash: Word? = null,
        fromOrderHash: Word? = null,
        newOrderVersion: OrderVersion? = null
    ): Flux<Order> {
        return LoggingUtils.withMarkerFlux { marker ->
            logger.info(marker, "update hash=$orderHash fromHash=$fromOrderHash")
            Flux.concat(
                orderVersionRepository.findAllByHash(orderHash, fromOrderHash).map { OrderUpdate.ByOrderVersion(it) },
                Mono.justOrEmpty<OrderVersion>(newOrderVersion).map { OrderUpdate.ByOrderVersion(it) },
                exchangeHistoryRepository.findLogEvents(orderHash, fromOrderHash).map { OrderUpdate.ByLogEvent(it) }
            ).windowUntilChanged { it.orderHash }.concatMap { updateOrder(it) }
        }
    }

    private sealed class OrderUpdate {
        abstract val orderHash: Word

        data class ByOrderVersion(val orderVersion: OrderVersion) : OrderUpdate() {
            override val orderHash get() = orderVersion.hash
        }

        data class ByLogEvent(val logEvent: LogEvent) : OrderUpdate() {
            override val orderHash get() = logEvent.data.toExchangeHistory().hash
        }
    }

    private fun updateOrder(updates: Flux<OrderUpdate>): Mono<Order> {
        return updates.reduce<Optional<Order>>(Optional.empty()) { order, update ->
            when (update) {
                is OrderUpdate.ByOrderVersion ->
                    order.map { it.updateWith(update.orderVersion) }
                        .or { Optional.of(update.orderVersion.toNewOrder()) }
                is OrderUpdate.ByLogEvent ->
                    order.map { it.updateWith(update.logEvent.status, update.logEvent.data.toExchangeHistory()) }
            }
        }.fromOptional().flatMap { updateOrderWithState(it) }
    }

    private fun Order.updateWith(logEventStatus: LogEventStatus, orderExchangeHistory: OrderExchangeHistory): Order {
        return when (logEventStatus) {
            LogEventStatus.PENDING -> copy(pending = pending + orderExchangeHistory)
            LogEventStatus.CONFIRMED -> when (orderExchangeHistory) {
                is OrderSideMatch -> copy(
                    fill = fill.plus(orderExchangeHistory.fill),
                    lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date)
                )
                is OrderCancel -> copy(
                    cancelled = true,
                    lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date)
                )
            }
            else -> this
        }
    }

    private fun Order.updateWith(orderVersion: OrderVersion): Order {
        validateUpdate(this, orderVersion)
        return copy(
            make = orderVersion.make,
            take = orderVersion.take,
            signature = orderVersion.signature,
            lastUpdateAt = orderVersion.createdAt
        )
    }

    private fun OrderVersion.toNewOrder() = toOrderExactFields().copy(
        priceHistory = listOf(createNotNormalizedPriceHistoryRecord(make, take, createdAt))
    )

    /**
     * Create a record that will be later processed by [priceNormalizer].
     */
    private fun createNotNormalizedPriceHistoryRecord(
        make: Asset,
        take: Asset,
        date: Instant
    ) = OrderPriceHistoryRecord(date, make.value.value.toBigDecimal(), take.value.value.toBigDecimal())

    private suspend fun Order.withNormalizedPriceHistoryRecords(): Order {
        val normalizedPriceHistories =
            priceHistory.sortedBy { it.date }.takeLast(Order.MAX_PRICE_HISTORIES).map { record ->
                OrderPriceHistoryRecord(
                    record.date,
                    priceNormalizer.normalize(Asset(make.type, EthUInt256(record.makeValue.toBigInteger()))),
                    priceNormalizer.normalize(Asset(take.type, EthUInt256(record.takeValue.toBigInteger())))
                )
            }
        return copy(priceHistory = normalizedPriceHistories)
    }

    private suspend fun Order.withUpdatedMakeStock(knownMakeBalance: EthUInt256? = null): Order {
        val makeBalance = knownMakeBalance ?: assetBalanceProvider.getAssetStock(maker, make.type) ?: EthUInt256.ZERO
        return withMakeBalance(makeBalance, protocolCommissionProvider.get())
    }

    private suspend fun Order.withNewPrice(): Order {
        val orderUsdValue = priceUpdateService.getAssetsUsdValue(make, take, nowMillis())
        return if (orderUsdValue != null) withOrderUsdValue(orderUsdValue) else this
    }

    private fun updateOrderWithState(order0: Order): Mono<Order> = mono {
        val order = order0
            .withNormalizedPriceHistoryRecords()
            .withUpdatedMakeStock()
            .withNewPrice()
        val version = orderRepository.findById(order.hash)?.version
        val saved = orderRepository.save(order.copy(version = version))
        logger.info(buildString {
            append("Updated order: ")
            append("hash=${saved.hash}, ")
            append("makeStock=${saved.makeStock}, ")
            append("fill=${saved.fill}, ")
            append("cancelled=${saved.cancelled}")
        })
        saved
    }

    class OrderUpdateError(val reason: OrderUpdateErrorReason) : RuntimeException("Order can't be updated: $reason") {
        enum class OrderUpdateErrorReason {
            CANCELLED,
            INVALID_UPDATE,
            MAKE_VALUE_ERROR,
            TAKE_VALUE_ERROR
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OrderReduceService::class.java)

        fun EventData.toExchangeHistory(): OrderExchangeHistory =
            requireNotNull(this as? OrderExchangeHistory) { "Unexpected exchange history type ${this::class}" }

        @Throws(OrderUpdateError::class)
        fun validateUpdate(existing: Order, update: OrderVersion) {
            if (existing.cancelled) {
                throw OrderUpdateError(OrderUpdateError.OrderUpdateErrorReason.CANCELLED)
            }
            if (existing.data != update.data) {
                throw OrderUpdateError(OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE)
            }
            if (existing.start != update.start) {
                throw OrderUpdateError(OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE)
            }
            if (existing.end != update.end) {
                throw OrderUpdateError(OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE)
            }
            if (existing.taker != update.taker) {
                throw OrderUpdateError(OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE)
            }
            if (update.make.value < existing.make.value) {
                throw OrderUpdateError(OrderUpdateError.OrderUpdateErrorReason.MAKE_VALUE_ERROR)
            }

            val newMaxTake = update.make.value * existing.take.value / existing.make.value
            if (newMaxTake < update.take.value) {
                throw OrderUpdateError(OrderUpdateError.OrderUpdateErrorReason.TAKE_VALUE_ERROR)
            }
        }
    }
}
