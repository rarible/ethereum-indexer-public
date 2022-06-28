package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.seaport.v1.OrderCancelledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.opensea.SeaportEventConverter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class SeaportV1ExchangeCancelDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val seaportEventConverter: SeaportEventConverter,
    private val seaportCancelEventCounter: RegisteredCounter,

) : LogEventDescriptor<OrderCancel> {

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = OrderCancelledEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<OrderCancel> {
        return mono { convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): List<OrderCancel> {
        val event = OrderCancelledEvent.apply(log)
        seaportCancelEventCounter.increment()
        return seaportEventConverter.convert(event, transaction, index, totalLogs, date)
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(
        listOf(exchangeContractAddresses.seaportV1)
    )
}
