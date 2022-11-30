package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.FeeUpdateEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolFeeUpdate
import com.rarible.protocol.order.listener.service.descriptors.PoolSubscriber
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import java.time.Instant
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
@CaptureSpan(type = SpanType.EVENT)
class SudoSwapFeeUpdatePairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapUpdateFeeEventCounter: RegisteredCounter
): PoolSubscriber<PoolFeeUpdate>(
    topic = FeeUpdateEvent.id(),
    contracts = emptyList()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<PoolFeeUpdate> {
        val event = FeeUpdateEvent.apply(log)
        return listOf(
            PoolFeeUpdate(
                hash = sudoSwapEventConverter.getPoolHash(log.address()),
                newFee = event.newFee(),
                date = timestamp,
                source = HistorySource.SUDOSWAP
            )
        ).also { sudoSwapUpdateFeeEventCounter.increment() }
    }
}
