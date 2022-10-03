package com.rarible.protocol.order.listener.service.descriptors.exchange.x2y2

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.x2y2.v1.events.EvInventoryEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import com.rarible.protocol.order.listener.service.x2y2.X2Y2EventConverter
import io.daonomic.rpc.domain.Word
import java.time.Instant
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
@CaptureSpan(type = SpanType.EVENT)
class X2Y2SellOrderMatchDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val converter: X2Y2EventConverter,
    private val x2y2MatchEventCounter: RegisteredCounter,
): ItemExchangeHistoryLogEventDescriptor<OrderSideMatch> {

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word
        get() = EvInventoryEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderSideMatch> {
        val event = EvInventoryEvent.apply(log)
        val converted = converter.convert(event, date, transaction.input())
        x2y2MatchEventCounter.increment()
        return converted
    }

    override fun getAddresses(): Mono<Collection<Address>> = listOf(exchangeContractAddresses.x2y2V1).toMono()
}
