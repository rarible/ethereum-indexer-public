package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NFTDepositEvent
import com.rarible.protocol.order.core.configuration.SudoSwapAddresses
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolNftDeposit
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
class SudoSwapDepositNftPairDescriptor(
    private val sudoSwapAddresses: SudoSwapAddresses,
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapDepositNftEventCounter: RegisteredCounter
): LogEventDescriptor<PoolNftDeposit> {

    override val collection: String = PoolHistoryRepository.COLLECTION

    override val topic: Word = NFTDepositEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = listOf(sudoSwapAddresses.pairFactoryV1).toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolNftDeposit> {
        return mono { listOfNotNull(convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): PoolNftDeposit {
        val event = NFTDepositEvent.apply(log)
        val details = sudoSwapEventConverter.getNftDepositDetails(log.address(), transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        return PoolNftDeposit(
            hash = sudoSwapEventConverter.getPoolHash(event.poolAddress()),
            collection = details.collection,
            tokenIds = details.tokenIds.map { EthUInt256.of(it) },
            date = date,
            source = HistorySource.SUDOSWAP
        ).apply { sudoSwapDepositNftEventCounter.increment() }
    }
}
