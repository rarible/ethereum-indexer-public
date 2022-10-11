package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.looksrare.v1.CancelMultipleOrdersEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV1ExchangeCancelDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    orderRepository: OrderRepository,
    looksrareCancelOrdersEventMetric: RegisteredCounter,
) : AbstractLooksrareExchangeDescriptor<OrderCancel>(
    orderRepository,
    looksrareCancelOrdersEventMetric
) {

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = CancelMultipleOrdersEvent.id()

    override fun convert(
        log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int
    ): Publisher<OrderCancel> {
        return convert(log, Instant.ofEpochSecond(timestamp))
    }

    private fun convert(log: Log, date: Instant): Flux<OrderCancel> {
        val event = CancelMultipleOrdersEvent.apply(log)
        val nonces = event.orderNonces().map { it.toLong() }
        val maker = event.user()
        return mono { cancelUserOrders(date, maker, nonces) }.flatMapMany { it.toFlux() }
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(
        listOf(exchangeContractAddresses.looksrareV1)
    )
}
