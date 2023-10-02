package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.looksrareError
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

abstract class AbstractLooksrareV1ExchangeTakerDescriptor(
    name: String,
    topic: Word,
    contracts: List<Address>,
    private val contractsProvider: ContractsProvider,
    orderRepository: OrderRepository,
    private val wrapperLooksrareMatchEventMetric: RegisteredCounter,
    private val tokenStandardProvider: TokenStandardProvider,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val metrics: ForeignOrderMetrics,
    autoReduceService: AutoReduceService,
) : AbstractLooksrareExchangeDescriptor<OrderExchangeHistory>(
    name,
    topic,
    contracts,
    orderRepository,
    metrics,
    autoReduceService,
) {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass::class.java)

    protected abstract fun getTakeEvent(log: Log): TakeEvent?

    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderExchangeHistory> {
        val event = getTakeEvent(log) ?: return emptyList()
        val collectionType = tokenStandardProvider.getTokenStandard(event.collection)
        val taker = OrderSideMatch.getRealTaker(event.taker, transaction)

        val (make, take) = run {
            val nft = Asset(
                type = when (collectionType) {
                    is TokenStandard.ERC721 -> Erc721AssetType(event.collection, event.tokenId)
                    is TokenStandard.ERC1155 -> Erc1155AssetType(event.collection, event.tokenId)
                    null -> {
                        logger.looksrareError("Can't determine collection standard ${event.collection}, tx=${transaction.hash()}")
                        if (event.amount == EthUInt256.ONE) Erc721AssetType(event.collection, event.tokenId)
                        else Erc1155AssetType(event.collection, event.tokenId)
                    }
                },
                value = event.amount
            )
            val currency = Asset(
                type = if (event.currency == contractsProvider.weth() || event.currency == Address.ZERO())
                    EthAssetType else Erc20AssetType(event.currency),
                value = event.price
            )
            if (event.isAsk) (nft to currency) else (currency to nft)
        }
        val leftUsdValue = priceUpdateService.getAssetsUsdValue(make, take, timestamp)
        val rightUsdValue = priceUpdateService.getAssetsUsdValue(take, make, timestamp)
        val events = listOf(
            OrderSideMatch(
                hash = event.orderHash,
                counterHash = keccak256(event.orderHash),
                side = OrderSide.LEFT,
                fill = event.amount,
                maker = event.maker,
                taker = taker,
                make = make,
                take = take,
                makeValue = prizeNormalizer.normalize(make),
                takeValue = prizeNormalizer.normalize(take),
                makeUsd = leftUsdValue?.makeUsd,
                takeUsd = leftUsdValue?.takeUsd,
                makePriceUsd = leftUsdValue?.makePriceUsd,
                takePriceUsd = leftUsdValue?.takePriceUsd,
                date = timestamp,
                source = HistorySource.LOOKSRARE,
                adhoc = false,
                counterAdhoc = true,
            ),
            OrderSideMatch(
                hash = keccak256(event.orderHash),
                counterHash = event.orderHash,
                side = OrderSide.RIGHT,
                fill = event.amount,
                maker = taker,
                taker = event.maker,
                make = take,
                take = make,
                makeValue = prizeNormalizer.normalize(take),
                takeValue = prizeNormalizer.normalize(make),
                makeUsd = rightUsdValue?.makeUsd,
                takeUsd = rightUsdValue?.takeUsd,
                makePriceUsd = rightUsdValue?.makePriceUsd,
                takePriceUsd = rightUsdValue?.takePriceUsd,
                date = timestamp,
                source = HistorySource.LOOKSRARE,
                adhoc = true,
                counterAdhoc = false,
            )
        )
        metrics.onOrderEventHandled(Platform.LOOKSRARE, "match")

        val matchEvents = OrderSideMatch.addMarketplaceMarker(
            events,
            transaction.input(),
            wrapperLooksrareMatchEventMetric
        )
        val cancelEvents = cancelUserOrders(timestamp, event.maker, listOf(event.orderNonce))
            // All orders with same nonce should be cancelled, except executed one
            .filter { it.hash != event.orderHash }

        return matchEvents + cancelEvents
    }

    protected data class TakeEvent(
        val maker: Address,
        val taker: Address,
        val orderHash: Word,
        val orderNonce: BigInteger,
        val currency: Address,
        val collection: Address,
        val tokenId: EthUInt256,
        val amount: EthUInt256,
        val price: EthUInt256,
        val isAsk: Boolean,
        val isNonceInvalidated: Boolean = false,
        val protocolFee: BigInteger? = null,
        val royalty: Royalty? = null
    )

    protected data class Royalty(
        val creator: Address,
        val amount: EthUInt256
    )
}
