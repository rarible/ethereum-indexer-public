package com.rarible.protocol.order.listener.service.descriptors.exchange.v1

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v1.BuyEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.asset.AssetTypeService
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import java.math.BigInteger
import java.time.Instant

@Service
class ExchangeBuyDescriptor(
    exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val assetTypeService: AssetTypeService,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer
) : ItemExchangeHistoryLogEventDescriptor<OrderSideMatch> {

    private val addresses = listOfNotNull(exchangeContractAddresses.v1, exchangeContractAddresses.v1Old)

    override val topic: Word = BuyEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OrderSideMatch> {
        val event = BuyEvent.apply(log)

        val makeAssetType = assetTypeService.toAssetType(event.sellToken(), EthUInt256(event.sellTokenId()));
        val make = Asset(makeAssetType, EthUInt256(event.amount()))

        val takeAssetType = assetTypeService.toAssetType(event.buyToken(), EthUInt256(event.buyTokenId()))
        val take = Asset(takeAssetType, EthUInt256(event.fill))

        val usdValue = priceUpdateService.getAssetsUsdValue(make, take, nowMillis())
        val hash = Order.hashKey(event.owner(), makeAssetType, takeAssetType, event.salt())
        val counterHash = Order.hashKey(event.buyer(), takeAssetType, makeAssetType, BigInteger.ZERO)

        val adhoc = false
        val counterAdhoc = true

        return listOf(
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
                date = date
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
                date = date
            )
        )
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(addresses)
    }
}

val BuyEvent.fill: BigInteger
    get() = amount().multiply(buyValue()).div(sellValue())
