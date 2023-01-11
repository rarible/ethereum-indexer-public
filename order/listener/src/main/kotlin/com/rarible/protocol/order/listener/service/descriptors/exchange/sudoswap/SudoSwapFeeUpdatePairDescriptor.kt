package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.FeeUpdateEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolFeeUpdate
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
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
@EnableSudoSwap
class SudoSwapFeeUpdatePairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapUpdateFeeEventCounter: RegisteredCounter
): LogEventDescriptor<PoolFeeUpdate> {

    override val collection: String = PoolHistoryRepository.COLLECTION

    override val topic: Word = FeeUpdateEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolFeeUpdate> {
        return mono { listOfNotNull(convert(log, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, date: Instant): PoolFeeUpdate {
        val event = FeeUpdateEvent.apply(log)
        return PoolFeeUpdate(
            hash = sudoSwapEventConverter.getPoolHash(log.address()),
            newFee = event.newFee(),
            date = date,
            source = HistorySource.SUDOSWAP
        ).also { sudoSwapUpdateFeeEventCounter.increment() }
    }
}
