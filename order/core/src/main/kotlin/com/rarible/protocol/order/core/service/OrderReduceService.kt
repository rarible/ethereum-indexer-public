package com.rarible.protocol.order.core.service

import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.time.Instant

@Component
class OrderReduceService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderRepository: OrderRepository,
    private val assetBalanceProvider: AssetBalanceProvider,
    private val protocolCommissionProvider: ProtocolCommissionProvider
) {
    fun onExchange(logEvents: List<LogEvent>): Mono<Void> {
        return logEvents
            .toFlux()
            .map { it.data.toExchangeHistory().hash }
            .distinct()
            .concatMap { update(it) }
            .then()
    }

    fun update(hash: Word? = null, from: Word? = null): Flux<Word> {
        return LoggingUtils.withMarkerFlux { marker ->
            logger.info(marker, "update hash=$hash from=$from")
            exchangeHistoryRepository.findLogEvents(hash, from)
                .windowUntilChanged { it.data.toExchangeHistory().hash  }
                .concatMap {
                    it.switchOnFirst { first, histories ->
                        logger.info(marker, "starter processing $first")
                        val firstHistory = first.get()

                        if (firstHistory != null) {
                            val currentHash = firstHistory.data.toExchangeHistory().hash
                            updateOrder(marker, currentHash, histories)
                                .thenReturn(currentHash)
                        } else {
                            histories.then(Mono.empty())
                        }
                    }
                }
        }
    }

    private fun updateOrder(marker: Marker, hash: Word, histories: Flux<LogEvent>): Mono<Word> {
        return histories.reduce(OrderState.initial()) { state, history ->
            val exchangeHistory = history.data.toExchangeHistory()
            when (history.status) {
                LogEventStatus.PENDING -> {
                    state.addPendingEvent(exchangeHistory)
                }
                LogEventStatus.CONFIRMED -> {
                    when (exchangeHistory) {
                        is OrderSideMatch -> state.addFill(exchangeHistory.fill, exchangeHistory.date)
                        is OrderCancel -> state.cancel(exchangeHistory.date)
                    }
                }
                else -> state
            }
        }
            .flatMap { updateOrderWithState(marker, hash, it) }
    }

    private fun updateOrderWithState(marker: Marker, hash: Word, currentState: OrderState) = mono {
        val order = orderRepository.findById(hash)
        if (order != null) {
            if (order.fill != currentState.fill ||
                order.cancelled != currentState.canceled ||
                order.pending != currentState.pending
            ) {
                val makeBalance = assetBalanceProvider.getAssetStock(order.maker, order.make.type) ?: EthUInt256.ZERO

                val updatedOrder = order.withFillAndCancelledAndPendingAndChangeDate(
                    fill = currentState.fill,
                    makeBalance = makeBalance,
                    protocolCommission = protocolCommissionProvider.get(),
                    cancelled = currentState.canceled,
                    pending = currentState.pending,
                    changeDate = currentState.changeDate
                )
                val savedOrder = orderRepository.save(updatedOrder)
                logger.info("Updated order: hash=${savedOrder.hash}, makeBalance=$makeBalance, makeStock=${savedOrder.makeStock}, fill=${savedOrder.fill}, cancelled=${savedOrder.cancelled}")
                savedOrder.hash
            } else {
                order.hash
            }
        } else {
            logger.info(marker, "order not found $hash")
            hash
        }
    }

    private fun EventData.toExchangeHistory(): OrderExchangeHistory {
        return this as? OrderExchangeHistory ?: throw IllegalArgumentException("Unexpected exchange history type ${this::class}")
    }

    data class OrderState(
        val fill: EthUInt256,
        val canceled: Boolean,
        val pending: List<OrderExchangeHistory>,
        val changeDate: Instant
    ) {
        fun addPendingEvent(event: OrderExchangeHistory): OrderState {
            return copy(pending = pending + event)
        }

        fun addFill(
            otherFill: EthUInt256,
            stateChangeDate: Instant
        ): OrderState {
            return copy(
                fill = fill.plus(otherFill),
                changeDate = getLatestChangeDate(stateChangeDate)
            )
        }

        fun cancel(stateChangeDate: Instant): OrderState {
            return copy(
                canceled = true,
                changeDate = getLatestChangeDate(stateChangeDate)
            )
        }

        private fun getLatestChangeDate(changeDate: Instant): Instant {
            return if (changeDate > this.changeDate) changeDate else this.changeDate
        }

        companion object {
            fun initial(): OrderState {
                return OrderState(EthUInt256.ZERO, false, emptyList(), Instant.ofEpochMilli(0))
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OrderReduceService::class.java)
    }
}
