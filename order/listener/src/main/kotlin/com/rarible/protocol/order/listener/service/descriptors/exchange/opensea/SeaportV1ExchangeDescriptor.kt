package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.opensea.SeaportEventConverter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class SeaportV1ExchangeDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val seaportEventConverter: SeaportEventConverter,
    private val seaportEventErrorCounter: RegisteredCounter,
    private val seaportFulfilledEventCounter: RegisteredCounter
) : LogEventDescriptor<OrderSideMatch> {

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = OrderFulfilledEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<OrderSideMatch> {
        return mono { convert(log, Instant.ofEpochSecond(timestamp), transaction) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, date: Instant, transaction: Transaction): List<OrderSideMatch> {
        val event = OrderFulfilledEvent.apply(log)
        val orderSideMatches = seaportEventConverter.convert(event, date, transaction)
        recordMetric(orderSideMatches, log)
        return orderSideMatches
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(
        listOf(exchangeContractAddresses.seaportV1)
    )

    private fun recordMetric(sideMatches: List<OrderSideMatch>, log: Log) {
        if (sideMatches.isEmpty()) {
            logger.warn("Can't convert event $log to order side matches")
            seaportEventErrorCounter.increment()
        } else {
            seaportFulfilledEventCounter.increment()
        }
    }


    private companion object {
        private val logger = LoggerFactory.getLogger(SeaportV1ExchangeDescriptor::class.java)
    }
}
