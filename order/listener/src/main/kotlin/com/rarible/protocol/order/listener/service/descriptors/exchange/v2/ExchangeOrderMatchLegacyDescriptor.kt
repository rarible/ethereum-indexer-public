package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.RaribleExchangeV2OrderParser
import com.rarible.protocol.order.listener.service.descriptors.getOriginMaker
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ExchangeOrderMatchLegacyDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val raribleOrderParser: RaribleExchangeV2OrderParser
) : LogEventDescriptor<OrderSideMatch> {

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = MatchEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<OrderSideMatch> {
        return mono { convert(log, transaction, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderSideMatch> {
        val event = MatchEvent.apply(log)
        val leftHash = Word.apply(event.leftHash())
        val rightHash = Word.apply(event.rightHash())
        val leftAssetType = event.leftAsset().toAssetType()
        val rightAssetType = event.rightAsset().toAssetType()

        val leftMake = Asset(leftAssetType, EthUInt256(event.newRightFill()))
        val leftTake = Asset(rightAssetType, EthUInt256(event.newLeftFill()))
        val leftUsdValue = priceUpdateService.getAssetsUsdValue(leftMake, leftTake, date)

        val rightMake = Asset(rightAssetType, EthUInt256(event.newLeftFill()))
        val rightTake = Asset(leftAssetType, EthUInt256(event.newRightFill()))
        val rightUsdValue = priceUpdateService.getAssetsUsdValue(rightMake, rightTake, date)

        val transactionOrders = raribleOrderParser.parseMatchedOrders(transaction.hash(), transaction.input(), event)
        val leftMaker = getOriginMaker(event.leftMaker(), transactionOrders?.left?.data)
        val rightMaker = getOriginMaker(event.rightMaker(), transactionOrders?.right?.data)
        val leftAdhoc = transactionOrders?.left?.salt == EthUInt256.ZERO
        val rightAdhoc = transactionOrders?.right?.salt == EthUInt256.ZERO

        val leftFill = if (transactionOrders?.left?.isMakeFillOrder == true) {
            EthUInt256(event.newRightFill())
        } else {
            EthUInt256(event.newLeftFill())
        }
        val rightFill = if (transactionOrders?.right?.isMakeFillOrder == true) {
            EthUInt256(event.newLeftFill())
        } else {
            EthUInt256(event.newRightFill())
        }
        return listOf(
            OrderSideMatch(
                hash = leftHash,
                counterHash = rightHash,
                side = OrderSide.LEFT,
                fill = leftFill,
                make = leftMake,
                take = leftTake,
                maker = leftMaker,
                taker = rightMaker,
                makeUsd = leftUsdValue?.makeUsd,
                takeUsd = leftUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(leftMake),
                takeValue = prizeNormalizer.normalize(leftTake),
                makePriceUsd = leftUsdValue?.makePriceUsd,
                takePriceUsd = leftUsdValue?.takePriceUsd,
                source = HistorySource.RARIBLE,
                date = date,
                adhoc = leftAdhoc,
                counterAdhoc = rightAdhoc,
                originFees = transactionOrders?.left?.originFees,
                marketplaceMarker = transactionOrders?.left?.marketplaceMarker
            ),
            OrderSideMatch(
                hash = rightHash,
                counterHash = leftHash,
                side = OrderSide.RIGHT,
                fill = rightFill,
                make = rightMake,
                take = rightTake,
                maker = rightMaker,
                taker = leftMaker,
                makeUsd = rightUsdValue?.makeUsd,
                takeUsd = rightUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(rightMake),
                takeValue = prizeNormalizer.normalize(rightTake),
                makePriceUsd = rightUsdValue?.makePriceUsd,
                takePriceUsd = rightUsdValue?.takePriceUsd,
                source = HistorySource.RARIBLE,
                date = date,
                adhoc = rightAdhoc,
                counterAdhoc = leftAdhoc,
                originFees = transactionOrders?.right?.originFees,
                marketplaceMarker = transactionOrders?.right?.marketplaceMarker
            )
        )
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(listOf(exchangeContractAddresses.v2))
    }
}

