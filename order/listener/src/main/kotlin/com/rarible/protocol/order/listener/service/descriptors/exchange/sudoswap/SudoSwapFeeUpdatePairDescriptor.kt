package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.FeeUpdateEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolFeeUpdate
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.listener.configuration.SudoSwapLoadProperties
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.PoolSubscriber
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@EnableSudoSwap
class SudoSwapFeeUpdatePairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapUpdateFeeEventCounter: RegisteredCounter,
    private val sudoSwapPoolInfoProvider: PoolInfoProvider,
    private val sudoSwapLoad: SudoSwapLoadProperties,
    autoReduceService: AutoReduceService,
) : PoolSubscriber<PoolFeeUpdate>(
    name = "sudo_fee_update",
    topic = FeeUpdateEvent.id(),
    contracts = emptyList(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<PoolFeeUpdate> {
        val event = FeeUpdateEvent.apply(log)
        val hash = sudoSwapEventConverter.getPoolHash(log.address())
        val poolInfo = sudoSwapPoolInfoProvider.getPollInfo(hash, log.address()) ?: return emptyList()
        if (poolInfo.collection in sudoSwapLoad.ignoreCollections) {
            return emptyList()
        }
        return listOf(
            PoolFeeUpdate(
                hash = hash,
                newFee = event.newFee(),
                date = timestamp,
                source = HistorySource.SUDOSWAP
            )
        ).also { sudoSwapUpdateFeeEventCounter.increment() }
    }
}
