package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NewPairEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolCreate
import com.rarible.protocol.order.core.model.SudoSwapCurveType
import com.rarible.protocol.order.core.model.SudoSwapErc20PairDetail
import com.rarible.protocol.order.core.model.SudoSwapEthPairDetail
import com.rarible.protocol.order.core.model.SudoSwapPoolDataV1
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.PoolSubscriber
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import org.springframework.stereotype.Service
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
@EnableSudoSwap
class SudoSwapCreatePairDescriptor(
    private val contractsProvider: ContractsProvider,
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapCreatePairEventCounter: RegisteredCounter
) : PoolSubscriber<PoolCreate>(
    name = "sudo_new_pair",
    topic = NewPairEvent.id(),
    contracts = contractsProvider.pairFactoryV1()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<PoolCreate> {
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
            return emptyList()
        }
        val curveType = when (details.bondingCurve) {
            contractsProvider.linearCurveV1() -> SudoSwapCurveType.LINEAR
            contractsProvider.exponentialCurveV1() -> SudoSwapCurveType.EXPONENTIAL
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
        return listOf(
            PoolCreate(
                hash = sudoSwapEventConverter.getPoolHash(event.poolAddress()),
                collection = details.nft,
                tokenIds = details.inNft.map { EthUInt256.of(it) },
                currency = currency,
                currencyBalance = balance,
                data = data,
                date = timestamp,
                source = HistorySource.SUDOSWAP
            )
        ).also { sudoSwapCreatePairEventCounter.increment() }
    }

    private companion object {
        val SUPPORTED_POOL_TYPES = listOf(SudoSwapPoolType.NFT, SudoSwapPoolType.TRADE)
    }
}
