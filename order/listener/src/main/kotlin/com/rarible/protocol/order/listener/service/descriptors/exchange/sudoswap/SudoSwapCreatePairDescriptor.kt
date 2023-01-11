package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NewPairEvent
import com.rarible.protocol.order.core.configuration.SudoSwapAddresses
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolCreate
import com.rarible.protocol.order.core.model.SudoSwapCurveType
import com.rarible.protocol.order.core.model.SudoSwapErc20PairDetail
import com.rarible.protocol.order.core.model.SudoSwapEthPairDetail
import com.rarible.protocol.order.core.model.SudoSwapPoolDataV1
import com.rarible.protocol.order.core.model.SudoSwapPoolType
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
class SudoSwapCreatePairDescriptor(
    private val sudoSwapAddresses: SudoSwapAddresses,
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapCreatePairEventCounter: RegisteredCounter
): LogEventDescriptor<PoolCreate> {

    override val collection: String = PoolHistoryRepository.COLLECTION

    override val topic: Word = NewPairEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = listOf(sudoSwapAddresses.pairFactoryV1).toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolCreate> {
        return mono { listOfNotNull(convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): PoolCreate? {
        val event = NewPairEvent.apply(log)
        val details = sudoSwapEventConverter.getCreatePairDetails(log.address(), transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        val (currency, balance) = when (details) {
            is SudoSwapEthPairDetail ->
                Address.ZERO() to details.ethBalance
            is SudoSwapErc20PairDetail ->
                details.token to details.tokenBalance
        }
        if (details.poolType !in SUPPORTED_POOL_TYPES) {
            return null
        }
        val curveType = when (details.bondingCurve) {
            sudoSwapAddresses.linearCurveV1 -> SudoSwapCurveType.LINEAR
            sudoSwapAddresses.exponentialCurveV1 -> SudoSwapCurveType.EXPONENTIAL
            else -> SudoSwapCurveType.UNKNOWN
        }
        val data = SudoSwapPoolDataV1(
            poolAddress = event.poolAddress(),
            bondingCurve = details.bondingCurve,
            factory = log.address(),
            curveType = curveType,
            assetRecipient = details.assetRecipient,
            poolType = details.poolType,
            spotPrice = details.spotPrice,
            delta = details.delta,
            fee = details.fee
        )
        return PoolCreate(
            hash = sudoSwapEventConverter.getPoolHash(event.poolAddress()),
            collection = details.nft,
            tokenIds = details.inNft.map { EthUInt256.of(it) },
            currency = currency,
            currencyBalance = balance,
            data = data,
            date = date,
            source = HistorySource.SUDOSWAP,
        ).also { sudoSwapCreatePairEventCounter.increment() }
    }

    private companion object {
        val SUPPORTED_POOL_TYPES = listOf(SudoSwapPoolType.NFT, SudoSwapPoolType.TRADE)
    }
}