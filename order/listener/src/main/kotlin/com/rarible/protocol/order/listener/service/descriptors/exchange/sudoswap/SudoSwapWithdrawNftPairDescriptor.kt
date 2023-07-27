package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.NFTWithdrawalEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolNftWithdraw
import com.rarible.protocol.order.listener.configuration.SudoSwapLoadProperties
import com.rarible.protocol.order.listener.service.descriptors.PoolSubscriber
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
@EnableSudoSwap
class SudoSwapWithdrawNftPairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapWithdrawNftEventCounter: RegisteredCounter,
    private val sudoSwapLoad: SudoSwapLoadProperties,
) : PoolSubscriber<PoolNftWithdraw>(
    name = "sudo_nft_withdrawal",
    topic = NFTWithdrawalEvent.id(),
    contracts = emptyList()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<PoolNftWithdraw> {
        if (log.address() in sudoSwapLoad.ignorePairs) {
            return emptyList()
        }
        val details = sudoSwapEventConverter.getNftWithdrawDetails(log.address(), transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        return listOf(
            PoolNftWithdraw(
                hash = sudoSwapEventConverter.getPoolHash(log.address()),
                collection = details.collection,
                tokenIds = details.nft.map { EthUInt256.of(it) },
                date = timestamp,
                source = HistorySource.SUDOSWAP
            )
        ).also { sudoSwapWithdrawNftEventCounter.increment() }
    }
}
