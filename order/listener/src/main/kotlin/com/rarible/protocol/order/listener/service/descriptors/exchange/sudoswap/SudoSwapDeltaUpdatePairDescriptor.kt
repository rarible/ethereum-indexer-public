package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.DeltaUpdateEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolDeltaUpdate
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import java.time.Instant
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
@CaptureSpan(type = SpanType.EVENT)
class SudoSwapDeltaUpdatePairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter
): LogEventDescriptor<PoolDeltaUpdate> {

    override val collection: String = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = DeltaUpdateEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolDeltaUpdate> {
        return mono { listOfNotNull(convert(log, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, date: Instant): PoolDeltaUpdate {
        val event = DeltaUpdateEvent.apply(log)
        return PoolDeltaUpdate(
            hash = sudoSwapEventConverter.getPoolHash(log.address()),
            newDelta = EthUInt256.of(event.newDelta()),
            date = date,
            source = HistorySource.SUDOSWAP
        )
    }
}
