package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SpotPriceUpdateEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolSpotPriceUpdate
import com.rarible.protocol.order.listener.service.descriptors.PoolSubscriber
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
@EnableSudoSwap
class SudoSwapSpotPriceUpdatePairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapUpdateSpotPriceEventCounter: RegisteredCounter
) : PoolSubscriber<PoolSpotPriceUpdate>(
    name = "sudo_spot_price_update",
    topic = SpotPriceUpdateEvent.id(),
    contracts = emptyList()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<PoolSpotPriceUpdate> {
        val event = SpotPriceUpdateEvent.apply(log)
        return listOf(
            PoolSpotPriceUpdate(
                hash = sudoSwapEventConverter.getPoolHash(log.address()),
                newSpotPrice = event.newSpotPrice(),
                date = timestamp,
                source = HistorySource.SUDOSWAP
            )
        ).also { sudoSwapUpdateSpotPriceEventCounter.increment() }
    }
}
