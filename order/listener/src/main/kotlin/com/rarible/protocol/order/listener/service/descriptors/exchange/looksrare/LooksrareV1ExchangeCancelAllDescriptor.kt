package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.looksrare.v1.CancelAllOrdersEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV1ExchangeCancelAllDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val looksrareCancelAllEventMetric: RegisteredCounter
) : LogEventDescriptor<ChangeNonceHistory> {

    override val collection: String
        get() = NonceHistoryRepository.COLLECTION

    override val topic: Word = CancelAllOrdersEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<ChangeNonceHistory> {
        return mono { convert(log, Instant.ofEpochSecond(timestamp)) }
    }

    private suspend fun convert(log: Log, date: Instant): ChangeNonceHistory {
        val event = CancelAllOrdersEvent.apply(log)
        return ChangeNonceHistory(
            maker = event.user(),
            newNonce = EthUInt256.of(event.newMinNonce()),
            date = date,
        ).also { looksrareCancelAllEventMetric.increment() }
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.looksrareV1))
}