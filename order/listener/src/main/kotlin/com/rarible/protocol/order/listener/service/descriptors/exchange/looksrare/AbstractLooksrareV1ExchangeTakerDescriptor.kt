package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.misc.looksrareError
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

abstract class AbstractLooksrareV1ExchangeTakerDescriptor(
    private val looksrareTakeEventMetric: RegisteredCounter,
    private val tokenStandardProvider: TokenStandardProvider,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val currencyContractAddresses: OrderIndexerProperties.CurrencyContractAddresses
) : LogEventDescriptor<OrderSideMatch> {

    private val logger = LoggerFactory.getLogger(javaClass::class.java)
    protected abstract fun getTakeEvent(log: Log): TakeEvent

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Long,
        index: Int,
        totalLogs: Int
    ): Publisher<OrderSideMatch> {
        return mono { convert(log, transaction, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderSideMatch> {
        val event = getTakeEvent(log)
        val collectionType = tokenStandardProvider.getTokenStandard(event.collection)

        val (make, take) = run {
            val nft = Asset(
                type = when (collectionType) {
                    is TokenStandard.ERC721 -> Erc721AssetType(event.collection, event.tokenId)
                    is TokenStandard.ERC1155 -> Erc1155AssetType(event.collection, event.tokenId)
                    null -> {
                        logger.looksrareError("Can't determine collection standard $collection, tx=${transaction.hash()}")
                        if (event.amount == EthUInt256.ONE) Erc721AssetType(event.collection, event.tokenId)
                        else Erc1155AssetType(event.collection, event.tokenId)
                    }
                },
                value = event.amount
            )
            val currency = Asset(
                type = if (event.currency == currencyContractAddresses.weth) EthAssetType else Erc20AssetType(event.currency),
                value = event.price
            )
            if (event.isAsk) (nft to currency) else (currency to nft)
        }
        val leftUsdValue = priceUpdateService.getAssetsUsdValue(make, take, date)
        val rightUsdValue = priceUpdateService.getAssetsUsdValue(take, make, date)
        val lastBytes = transaction.input().bytes().takeLast(32)
        val marketplaceMarker = lastBytes
            .takeIf { it.takeLast(8) == OrderSideMatch.CALL_DATA_MARKER }
            ?.toByteArray()
            ?.let { Word.apply(it) }
        return listOf(
            OrderSideMatch(
                hash = event.orderHash,
                counterHash = keccak256(event.orderHash),
                side = OrderSide.LEFT,
                fill = event.amount,
                maker = event.maker,
                taker = event.taker,
                make = make,
                take = take,
                makeValue = prizeNormalizer.normalize(make),
                takeValue = prizeNormalizer.normalize(take),
                makeUsd = leftUsdValue?.makeUsd,
                takeUsd = leftUsdValue?.takeUsd,
                makePriceUsd = leftUsdValue?.makePriceUsd,
                takePriceUsd = leftUsdValue?.takePriceUsd,
                date = date,
                source = HistorySource.LOOKSRARE,
                adhoc = false,
                counterAdhoc = true,
            ),
            OrderSideMatch(
                hash = keccak256(event.orderHash),
                counterHash = event.orderHash,
                side = OrderSide.RIGHT,
                fill = event.amount,
                maker = event.taker,
                taker = event.maker,
                make = take,
                take = make,
                makeValue = prizeNormalizer.normalize(take),
                takeValue = prizeNormalizer.normalize(make),
                makeUsd = rightUsdValue?.makeUsd,
                takeUsd = rightUsdValue?.takeUsd,
                makePriceUsd = rightUsdValue?.makePriceUsd,
                takePriceUsd = rightUsdValue?.takePriceUsd,
                date = date,
                source = HistorySource.LOOKSRARE,
                adhoc = true,
                counterAdhoc = false,
                marketplaceMarker = marketplaceMarker
            )
        ).also { looksrareTakeEventMetric.increment() }
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(
        listOf(exchangeContractAddresses.looksrareV1)
    )

    protected data class TakeEvent(
        val maker: Address,
        val taker: Address,
        val orderHash: Word,
        val currency: Address,
        val collection: Address,
        val tokenId: EthUInt256,
        val amount: EthUInt256,
        val price: EthUInt256,
        val isAsk: Boolean
    )
}