package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v2.events.CancelEvent
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class ExchangeV2CancelDescriptor(
    contractsProvider: ContractsProvider,
    private val raribleCancelEventMetric: RegisteredCounter,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderCancel>(
    name = "rari_v2_cancel",
    topic = CancelEvent.id(),
    contracts = contractsProvider.raribleExchangeV2(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = CancelEvent.apply(log)
        val hash = Word.apply(event.hash())
        val makeType = event.makeAssetType().toAssetType()
        val takeType = event.takeAssetType().toAssetType()

        return listOf(
            OrderCancel(
                hash = hash,
                make = Asset(makeType, EthUInt256.ZERO),
                take = Asset(takeType, EthUInt256.ZERO),
                date = timestamp,
                maker = event.maker(),
                source = HistorySource.RARIBLE
            )
        ).also { raribleCancelEventMetric.increment() }
    }
}
