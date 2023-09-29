package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEventDeprecated
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.RaribleExchangeV2OrderParser
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.descriptors.getOriginMaker
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ExchangeOrderMatchDeprecatedDescriptor(
    contractsProvider: ContractsProvider,
    private val orderRepository: OrderRepository,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val raribleOrderParser: RaribleExchangeV2OrderParser,
    private val raribleMatchEventMetric: RegisteredCounter,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderSideMatch>(
    name = "rari_v2_match_deprecated",
    topic = MatchEventDeprecated.id(),
    contracts = contractsProvider.raribleExchangeV2(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderSideMatch> {
        val event = MatchEventDeprecated.apply(log)
        val leftHash = Word.apply(event.leftHash())
        val rightHash = Word.apply(event.rightHash())
        val leftOrder = orderRepository.findById(leftHash) ?: return emptyList()

        val leftMake = Asset(leftOrder.make.type, EthUInt256(event.newRightFill()))
        val leftTake = Asset(leftOrder.take.type, EthUInt256(event.newLeftFill()))
        val lestUsdValue = priceUpdateService.getAssetsUsdValue(leftMake, leftTake, timestamp)

        val rightMake = Asset(leftOrder.take.type, EthUInt256(event.newLeftFill()))
        val rightTake = Asset(leftOrder.make.type, EthUInt256(event.newRightFill()))
        val rightUsdValue = priceUpdateService.getAssetsUsdValue(rightMake, rightTake, timestamp)

        val transactionOrders = raribleOrderParser.parseMatchedOrders(transaction.hash(), transaction.input(), MatchEvent.apply(log))
        val leftMaker = getOriginMaker(event.leftMaker(), transactionOrders?.left?.data)
        val rightMaker = getOriginMaker(event.rightMaker(), transactionOrders?.right?.data)
        val leftAdhoc = transactionOrders?.left?.salt == EthUInt256.ZERO
        val rightAdhoc = transactionOrders?.right?.salt == EthUInt256.ZERO

        val events = listOf(
            OrderSideMatch(
                hash = leftHash,
                counterHash = rightHash,
                side = OrderSide.LEFT,
                fill = EthUInt256(event.newLeftFill()),
                make = leftMake,
                take = leftTake,
                maker = leftMaker,
                taker = rightMaker,
                makeUsd = lestUsdValue?.makeUsd,
                takeUsd = lestUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(leftMake),
                takeValue = prizeNormalizer.normalize(leftTake),
                makePriceUsd = lestUsdValue?.makePriceUsd,
                takePriceUsd = lestUsdValue?.takePriceUsd,
                source = HistorySource.RARIBLE,
                date = timestamp,
                data = transactionOrders?.left?.data,
                adhoc = leftAdhoc,
                counterAdhoc = rightAdhoc,
            ),
            OrderSideMatch(
                hash = rightHash,
                counterHash = leftHash,
                side = OrderSide.RIGHT,
                fill = EthUInt256(event.newRightFill()),
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
                date = timestamp,
                data = transactionOrders?.right?.data,
                adhoc = rightAdhoc,
                counterAdhoc = leftAdhoc,
            ).also { raribleMatchEventMetric.increment() }
        )
        return OrderSideMatch.addMarketplaceMarker(events, transaction.input())
    }
}
