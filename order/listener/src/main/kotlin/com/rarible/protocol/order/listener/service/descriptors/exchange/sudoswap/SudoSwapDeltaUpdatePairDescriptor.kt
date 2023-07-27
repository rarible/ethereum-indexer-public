package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.DeltaUpdateEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolDeltaUpdate
import com.rarible.protocol.order.listener.service.descriptors.PoolSubscriber
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
@EnableSudoSwap
class SudoSwapDeltaUpdatePairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapUpdateDeltaEventCounter: RegisteredCounter
) : PoolSubscriber<PoolDeltaUpdate>(
    name = "sudo_delta_update",
    topic = DeltaUpdateEvent.id(),
    contracts = emptyList()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<PoolDeltaUpdate> {
        val event = DeltaUpdateEvent.apply(log)
        return listOf(
            PoolDeltaUpdate(
                hash = sudoSwapEventConverter.getPoolHash(log.address()),
                newDelta = event.newDelta(),
                date = timestamp,
                source = HistorySource.SUDOSWAP
            )
        ).also { sudoSwapUpdateDeltaEventCounter.increment() }
    }
}
