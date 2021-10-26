package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

interface ItemExchangeHistoryLogEventDescriptor<T : OrderExchangeHistory> : LogEventDescriptor<T> {
    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override fun convert(log: Log, transaction: Transaction, timestamp: Long): Publisher<T> {
        return mono { convert(log, transaction, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<T>
}
