package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.DeltaUpdateEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolDeltaUpdate
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
class SudoSwapDeltaUpdatePairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapUpdateDeltaEventCounter: RegisteredCounter,
    private val sudoSwapPoolInfoProvider: PoolInfoProvider,
    private val sudoSwapLoad: SudoSwapLoadProperties,
    autoReduceService: AutoReduceService,
) : PoolSubscriber<PoolDeltaUpdate>(
    name = "sudo_delta_update",
    topic = DeltaUpdateEvent.id(),
    contracts = emptyList(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Instant,
        index: Int,
        totalLogs: Int
    ): List<PoolDeltaUpdate> {
        val event = DeltaUpdateEvent.apply(log)
        val hash = sudoSwapEventConverter.getPoolHash(log.address())
        val poolInfo = sudoSwapPoolInfoProvider.getPollInfo(hash, log.address()) ?: return emptyList()
        if (poolInfo.collection in sudoSwapLoad.ignoreCollections) {
            return emptyList()
        }
        return listOf(
            PoolDeltaUpdate(
                hash = hash,
                newDelta = event.newDelta(),
                date = timestamp,
                source = HistorySource.SUDOSWAP
            )
        ).also { sudoSwapUpdateDeltaEventCounter.increment() }
    }
}
