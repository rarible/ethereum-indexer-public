package com.rarible.protocol.order.listener.service.descriptors.exchange.v1

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v1.CancelEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.service.asset.AssetTypeService
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ExchangeCancelDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val assetTypeService: AssetTypeService
) : ItemExchangeHistoryLogEventDescriptor<OrderCancel> {

    override val topic: Word = CancelEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderCancel> {
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
                date = date
            )
        )
    }

    override fun getAddresses(): Mono<Collection<Address>> =
        Mono.just(listOfNotNull(exchangeContractAddresses.v1, exchangeContractAddresses.v1Old))
}
