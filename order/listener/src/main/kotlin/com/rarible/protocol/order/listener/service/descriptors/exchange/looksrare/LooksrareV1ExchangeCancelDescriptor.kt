package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.looksrare.v1.CancelMultipleOrdersEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV1ExchangeCancelDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val looksrareCancelOrdersEventMetric: RegisteredCounter,
    private val orderRepository: OrderRepository
) : LogEventDescriptor<OrderCancel> {

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = CancelMultipleOrdersEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<OrderCancel> {
        return convert(log, Instant.ofEpochSecond(timestamp)).asFlux()
    }

    private fun convert(log: Log, date: Instant): Flow<OrderCancel> {
        val event = CancelMultipleOrdersEvent.apply(log)
        val nonces = event.orderNonces().map { it.toLong() }
        val maker = event.user()
        looksrareCancelOrdersEventMetric.increment()
        return orderRepository.findByMakeAndByCounters(Platform.LOOKSRARE, maker, nonces).map {
            OrderCancel(
                hash = it.hash,
                maker = it.maker,
                make = it.make,
                take = it.take,
                date = date,
                source= HistorySource.LOOKSRARE
            )
        }
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(
        listOf(exchangeContractAddresses.looksrareV1)
    )
}
