package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTInPairEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolTargetNftIn
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import com.rarible.protocol.order.core.service.curve.SudoSwapCurve
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapPoolInfoProvider
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
class SudoSwapInNftPairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapInNftEventCounter: RegisteredCounter,
    private val sudoSwapPoolInfoProvider: SudoSwapPoolInfoProvider,
    private val sudoSwapCurve: SudoSwapCurve,
): LogEventDescriptor<PoolTargetNftIn> {

    override val collection: String = PoolHistoryRepository.COLLECTION

    override val topic: Word = SwapNFTInPairEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolTargetNftIn> {
        return mono { convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): List<PoolTargetNftIn> {
        val details = sudoSwapEventConverter.getSwapInNftDetails(log.address(), transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        val poolInfo = sudoSwapPoolInfoProvider.gePollInfo(log.address())
        val outputValue = sudoSwapCurve.getBuyInputValues(
            curve = poolInfo.curve,
            spotPrice = poolInfo.spotPrice,
            delta = poolInfo.delta,
            numItems = details.tokenIds.size,
        )
        return details.tokenIds.mapIndexed { i, tokenId ->
            PoolTargetNftIn(
                hash = sudoSwapEventConverter.getPoolHash(log.address()),
                collection = poolInfo.collection,
                tokenIds = listOf(EthUInt256.of(tokenId)),
                tokenRecipient = details.tokenRecipient,
                inputValue = EthUInt256.of(outputValue[i].value),
                date = date,
                source = HistorySource.SUDOSWAP
            )
        }.also { sudoSwapInNftEventCounter.increment() }
    }
}
