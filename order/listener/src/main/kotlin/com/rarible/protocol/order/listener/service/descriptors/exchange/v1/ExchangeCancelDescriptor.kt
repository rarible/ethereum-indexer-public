package com.rarible.protocol.order.listener.service.descriptors.exchange.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v1.CancelEvent
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.asset.AssetTypeService
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class ExchangeCancelDescriptor(
    contractsProvider: ContractsProvider,
    private val assetTypeService: AssetTypeService,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderCancel>(
    name = "rari_v1_cancel",
    topic = CancelEvent.id(),
    contracts = contractsProvider.raribleExchangeV1(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = CancelEvent.apply(log)

        val makeAssetType = assetTypeService.toAssetType(event.sellToken(), EthUInt256(event.sellTokenId()))
        val takeAssetType = assetTypeService.toAssetType(event.buyToken(), EthUInt256(event.buyTokenId()))
        val hash = Order.hashKey(event.owner(), makeAssetType, takeAssetType, event.salt())

        return listOf(
            OrderCancel(
                hash = hash,
                make = Asset(makeAssetType, EthUInt256.ZERO),
                take = Asset(takeAssetType, EthUInt256.ZERO),
                maker = event.owner(),
                source = HistorySource.RARIBLE,
                date = timestamp
            )
        )
    }
}
