package com.rarible.protocol.order.listener.service.descriptors.exchange.v1

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v1.BuyEvent
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.asset.AssetTypeService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ExchangeBuyDescriptor(
    contractsProvider: ContractsProvider,
    private val assetTypeService: AssetTypeService,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer
) : ExchangeSubscriber<OrderSideMatch>(
    name = "rari_v1_buy",
    topic = BuyEvent.id(),
    contracts = contractsProvider.raribleExchangeV1()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderSideMatch> {
        val event = BuyEvent.apply(log)

        val makeAssetType = assetTypeService.toAssetType(event.sellToken(), EthUInt256(event.sellTokenId()))
        val make = Asset(makeAssetType, EthUInt256(event.amount()))

        val takeAssetType = assetTypeService.toAssetType(event.buyToken(), EthUInt256(event.buyTokenId()))
        val take = Asset(takeAssetType, EthUInt256(event.fill))

        val usdValue = priceUpdateService.getAssetsUsdValue(make, take, timestamp)
        val hash = Order.hashKey(event.owner(), makeAssetType, takeAssetType, event.salt())
        val counterHash = Order.hashKey(event.buyer(), takeAssetType, makeAssetType, BigInteger.ZERO)

        val adhoc = false
        val counterAdhoc = true

        val events = listOf(
            OrderSideMatch(
                hash = hash,
                counterHash = counterHash,
                side = OrderSide.LEFT,
                fill = EthUInt256(event.fill),
                make = make,
                take = take,
                maker = event.owner(),
                taker = event.buyer(),
                makeUsd = usdValue?.makeUsd,
                takeUsd = usdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(make),
                takeValue = prizeNormalizer.normalize(take),
                makePriceUsd = usdValue?.makePriceUsd,
                takePriceUsd = usdValue?.takePriceUsd,
                source = HistorySource.RARIBLE,
                adhoc = adhoc,
                counterAdhoc = counterAdhoc,
                date = timestamp
            ),
            OrderSideMatch(
                hash = counterHash,
                counterHash = hash,
                side = OrderSide.RIGHT,
                fill = EthUInt256(event.amount()),
                make = take,
                take = make,
                maker = event.buyer(),
                taker = event.owner(),
                makeUsd = usdValue?.takeUsd,
                takeUsd = usdValue?.makeUsd,
                makeValue = prizeNormalizer.normalize(take),
                takeValue = prizeNormalizer.normalize(make),
                makePriceUsd = usdValue?.takePriceUsd,
                takePriceUsd = usdValue?.makePriceUsd,
                source = HistorySource.RARIBLE,
                adhoc = counterAdhoc,
                counterAdhoc = adhoc,
                date = timestamp,
            )
        )
        return OrderSideMatch.addMarketplaceMarker(events, transaction.input())
    }
}

val BuyEvent.fill: BigInteger
    get() = amount().multiply(buyValue()).div(sellValue())
