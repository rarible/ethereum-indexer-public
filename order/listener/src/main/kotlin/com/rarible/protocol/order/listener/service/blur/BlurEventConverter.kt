package com.rarible.protocol.order.listener.service.blur

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.blur.v1.evemts.OrdersMatchedEvent
import com.rarible.protocol.contracts.exchange.blur.v1.BlurV1
import com.rarible.protocol.contracts.exchange.blur.v1.OrderCancelledEvent
import com.rarible.protocol.contracts.exchange.blur.v2.BlurV2
import com.rarible.protocol.contracts.exchange.wyvern.NonceIncrementedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.BlurExecution
import com.rarible.protocol.order.core.model.BlurOrder
import com.rarible.protocol.order.core.model.BlurOrderSide
import com.rarible.protocol.order.core.model.ChangeNonceHistory
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
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.converter.AbstractEventConverter
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Component
class BlurEventConverter(
    traceCallService: TraceCallService,
    featureFlags: OrderIndexerProperties.FeatureFlags,
    private val standardProvider: TokenStandardProvider,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val nonceHistoryRepository: NonceHistoryRepository,
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses
) : AbstractEventConverter(traceCallService, featureFlags) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convertToSideMatch(
        log: Log,
        transaction: Transaction,
        index: Int,
        totalLogs: Int,
        date: Instant,
    ): List<OrderSideMatch> {
        val txHash = transaction.hash()
        val executions = getExecutions(log, transaction)
        val event = OrdersMatchedEvent.apply(log)

        val execution = if (executions.size == totalLogs) {
            executions[index]
        } else {
            findExecution(executions, event, txHash) ?: return emptyList()
        }
        val sellOrder = BlurOrderParser.convert(event.sell())
        val sellHash = Word.apply(event.sellHash())
        val isSellAdhoc = execution.sell.isEmptySignature()

        val buyOrder = BlurOrderParser.convert(event.buy())
        val buyHash = Word.apply(event.buyHash())
        val isBuyAdhoc = execution.buy.isEmptySignature()

        val sellAssets = getOrderAssets(sellOrder)
        val buyAssets = getOrderAssets(buyOrder)

        val sellUsdValue = priceUpdateService.getAssetsUsdValue(make = sellAssets.make, take = sellAssets.take, at = date)
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
        return OrderSideMatch.addMarketplaceMarker(events, transaction.input())
    }

    private suspend fun findExecution(
        executions: List<BlurExecution>,
        event: OrdersMatchedEvent,
        txHash: Word?
    ): BlurExecution? {
        val sellHash = Word.apply(event.sellHash())
        val buyHash = Word.apply(event.buyHash())

        val foundExecutions = executions.filter { execution ->
            val sellMatched = foundHashMatch(execution.sell.order, sellHash)
            val buyMatched = foundHashMatch(execution.buy.order, buyHash)
            sellMatched && buyMatched
        }
        return if (foundExecutions.size != 1) {
            logger.error("Can't find execution for tx $txHash")
            null
        } else foundExecutions.single()
    }

    private suspend fun foundHashMatch(order: BlurOrder, hash: Word?): Boolean {
        val trader = order.trader
        val maxCounter = nonceHistoryRepository
            .findLatestNonceHistoryByMaker(trader, exchangeContractAddresses.blurV1)
            ?.let { it.data as? ChangeNonceHistory }?.newNonce?.value?.toLong() ?: 0L

        for (nonce in maxCounter downTo 0L) {
            if (order.hash(nonce) == hash) {
                return true
            }
        }
        return false
    }

    suspend fun convertToCancel(
        log: Log,
        transaction: Transaction,
        index: Int,
        totalLogs: Int,
        date: Instant
    ): List<OrderCancel> {
        val txHash = transaction.hash()
        val event = OrderCancelledEvent.apply(log)
        val blurOrders = getMethodInput(
            event.log(),
            transaction,
            BlurV1.cancelOrderSignature().id(),
            BlurV1.cancelOrdersSignature().id()
        ).map { BlurOrderParser.parserOrder(it, txHash) }.flatten()
        require(blurOrders.size == totalLogs) {
            "Canceled orders in tx $txHash didn't match total events, orders=${blurOrders.size}, totalLogs=$totalLogs"
        }
        return blurOrders[index].let {
            val assets = getOrderAssets(it)
            listOf(
                OrderCancel(
                    hash = Word.apply(event.hash()),
                    maker = it.trader,
                    make = assets.make,
                    take = assets.take,
                    date = date,
                    source = HistorySource.BLUR
                )
            )
        }
    }

    suspend fun convertChangeNonce(
        log: Log,
        date: Instant
    ): List<ChangeNonceHistory> {
        val event = NonceIncrementedEvent.apply(log)
        return listOf(
            ChangeNonceHistory(
                maker = event.maker(),
                newNonce = EthUInt256.of(event.newNonce()),
                date = date,
                source = HistorySource.BLUR
            )
        )
    }

    private suspend fun getOrderAssets(order: BlurOrder): OrderAssets {
        val nft = getNftAsset(order)
        val currency = getCurrencyAsset(order)
        val (make, take) = when (order.side) {
            BlurOrderSide.SELL -> nft to currency
            BlurOrderSide.BUY -> currency to nft
        }
        return OrderAssets(make, take)
    }

    private suspend fun getNftAsset(order: BlurOrder): Asset {
        val collection = order.collection
        val tokenId = EthUInt256.of(order.tokenId)
        val value = EthUInt256.of(order.amount)
        val standard = standardProvider.getTokenStandard(collection) ?: edgeCase(collection, order.amount)
        return when (standard) {
            TokenStandard.ERC1155 -> Erc1155AssetType(collection, tokenId)
            TokenStandard.ERC721 -> Erc721AssetType(collection, tokenId)
        }.let { Asset(it, value) }
    }

    // It should happen very rarely
    private fun edgeCase(collection: Address, value: BigInteger): TokenStandard {
        val standard = when (value) {
            BigInteger.ONE -> TokenStandard.ERC721
            else -> TokenStandard.ERC1155
        }
        logger.warn("Standard is unknown for $collection set to $standard")
        return standard
    }

    private suspend fun getCurrencyAsset(order: BlurOrder): Asset {
        return when (val paymentToken = order.paymentToken) {
            Address.ZERO() -> EthAssetType
            else -> Erc20AssetType(paymentToken)
        }.let { Asset(it, EthUInt256.of(order.price)) }
    }

    private suspend fun getExecutions(log: Log, transaction: Transaction): List<BlurExecution> {
        val txHash = transaction.hash()
        val txInput = transaction.input()
        return when (txInput.methodSignatureId()) {
            BlurV2.batchBuyWithETHSignature(),
            BlurV2.batchBuyWithERC20sSignature() -> {
                if (featureFlags.parseBlurMarketPlaceV2) {
                    val executions = BlurOrderParser.parseTradeDetails(txInput, log.transactionHash())
                    BlurOrderParser.tryFetchExecutions(executions.map { it.tradeData }, txHash)
                } else emptyList()
            }
            else -> emptyList()
        }.ifEmpty {
            getMethodInput(
                log,
                transaction,
                BlurV1.executeSignature().id(),
                BlurV1.bulkExecuteSignature().id(),
            ).map { BlurOrderParser.parseExecutions(it, txHash) }.flatten()
        }
    }

    private data class OrderAssets(
        val make: Asset,
        val take: Asset
    )
}
