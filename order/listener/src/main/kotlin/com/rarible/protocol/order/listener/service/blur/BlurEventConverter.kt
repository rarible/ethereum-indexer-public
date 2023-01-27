package com.rarible.protocol.order.listener.service.blur

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.blur.v1.evemts.OrdersMatchedEvent
import com.rarible.protocol.contracts.exchange.blur.v1.BlurV1
import com.rarible.protocol.contracts.exchange.blur.v1.OrderCancelledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.BlurOrder
import com.rarible.protocol.order.core.model.BlurOrderSide
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.parser.BlurOrderParser
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.converter.AbstractEventConverter
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Component
class BlurEventConverter(
    traceCallService: TraceCallService,
    featureFlags: OrderIndexerProperties.FeatureFlags,
    private val standardProvider: TokenStandardProvider,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
) : AbstractEventConverter(traceCallService, featureFlags) {

    suspend fun convert(
        log: Log,
        date: Instant,
        input: Bytes,
    ): List<OrderSideMatch> {
        val event = OrdersMatchedEvent.apply(log)

        val sellOrder = BlurOrderParser.convert(event.sell())
        val sellHash = Word.apply(event.sellHash())
        val isSellAdhoc = event.maker() != sellOrder.trader

        val buyOrder = BlurOrderParser.convert(event.buy())
        val buyHash = Word.apply(event.buyHash())
        val isBuyAdhoc = isSellAdhoc.not()

        val sellAssets = getOrderAssets(sellOrder)
        val buyAssets = getOrderAssets(buyOrder)

        val sellUsdValue =
            priceUpdateService.getAssetsUsdValue(make = sellAssets.make, take = sellAssets.take, at = date)
        val buyUsdValue = priceUpdateService.getAssetsUsdValue(make = buyAssets.make, take = buyAssets.take, at = date)

        val events = listOf(
            OrderSideMatch(
                hash = sellHash,
                counterHash = buyHash,
                maker = sellOrder.trader,
                taker = buyOrder.trader,
                side = OrderSide.LEFT,
                make = sellAssets.make,
                take = sellAssets.take,
                fill = sellAssets.take.value,
                makeUsd = sellUsdValue?.makeUsd,
                takeUsd = sellUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(sellAssets.make),
                takeValue = prizeNormalizer.normalize(sellAssets.take),
                makePriceUsd = sellUsdValue?.makePriceUsd,
                takePriceUsd = sellUsdValue?.takePriceUsd,
                source = HistorySource.BLUR,
                date = date,
                adhoc = isSellAdhoc,
                counterAdhoc = isBuyAdhoc,
                origin = null,
                originFees = null,
                externalOrderExecutedOnRarible = null,
            ),
            OrderSideMatch(
                hash = buyHash,
                counterHash = sellHash,
                maker = buyOrder.trader,
                taker = sellOrder.trader,
                side = OrderSide.RIGHT,
                make = buyAssets.make,
                take = buyAssets.take,
                fill = buyAssets.take.value,
                makeUsd = buyUsdValue?.makeUsd,
                takeUsd = buyUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(buyAssets.make),
                takeValue = prizeNormalizer.normalize(buyAssets.take),
                makePriceUsd = buyUsdValue?.makePriceUsd,
                takePriceUsd = buyUsdValue?.takePriceUsd,
                source = HistorySource.BLUR,
                date = date,
                adhoc = isBuyAdhoc,
                counterAdhoc = isSellAdhoc,
                origin = null,
                originFees = null,
                externalOrderExecutedOnRarible = null,
            )
        )
        return OrderSideMatch.addMarketplaceMarker(events, input)
    }

    suspend fun convert(
        log: Log,
        transaction: Transaction,
        index: Int,
        totalLogs: Int,
        date: Instant
    ): List<OrderCancel> {
        val event = OrderCancelledEvent.apply(log)
        val inputs = getMethodInput(
            event.log(),
            transaction,
            BlurV1.cancelOrderSignature().id(), BlurV1.cancelOrderSignature().id()
        )
        require(inputs.size == totalLogs) {
            "Canceled orders in tx ${transaction.hash()} didn't match total events, inputs=${inputs.size}, totalLogs=$totalLogs"
        }
        return BlurOrderParser.parserOrder(inputs[index]).map {
            val assets = getOrderAssets(it)
            OrderCancel(
                hash = Word.apply(event.hash()),
                maker = it.trader,
                make = assets.make,
                take = assets.take,
                date = date,
                source = HistorySource.BLUR
            )
        }
    }

    private suspend fun getOrderAssets(order: BlurOrder): OrderAssets {
        val nft = getNftAsset(order)
        val currency = getCurrencyAsset(order)
        val (make, take) = when (order.side) {
            BlurOrderSide.SELL -> nft to currency
            BlurOrderSide.BUY ->  currency to nft
        }
        return OrderAssets(make, take)
    }

    private suspend fun getNftAsset(order: BlurOrder): Asset {
        val collection = order.collection
        val tokenId = EthUInt256.of(order.tokenId)
        val value = EthUInt256.of(order.amount)
        return when (standardProvider.getTokenStandard(collection)) {
            TokenStandard.ERC1155 -> Erc1155AssetType(collection, tokenId)
            TokenStandard.ERC721 -> Erc721AssetType(collection, tokenId)
            null -> throw IllegalArgumentException("Invalid token standard for $collection")
        }.let { Asset(it, value) }
    }

    private suspend fun getCurrencyAsset(order: BlurOrder): Asset {
        return when (val paymentToken = order.paymentToken) {
            Address.ZERO() -> EthAssetType
            else -> Erc20AssetType(paymentToken)
        }.let { Asset(it, EthUInt256.of(order.price)) }
    }

    private data class OrderAssets(
        val make: Asset,
        val take: Asset
    )
}