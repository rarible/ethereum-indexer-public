package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.v2.events.MatchEventDeprecated
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.service.order.SideMatchTransactionProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
class ExchangeOrderMatchDeprecatedDescriptor(
    exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val orderRepository: OrderRepository,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val sideMatchTransactionProvider: SideMatchTransactionProvider
) : LogEventDescriptor<OrderSideMatch> {

    private val exchangeContract = exchangeContractAddresses.v2

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = MatchEventDeprecated.id()

    override fun convert(log: Log, timestamp: Long): Publisher<OrderSideMatch> {
        return mono { convert(log, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, date: Instant): List<OrderSideMatch> {
        val event = MatchEventDeprecated.apply(log)
        val leftHash = Word.apply(event.leftHash())
        val rightHash = Word.apply(event.rightHash())
        val leftOrder = orderRepository.findById(leftHash) ?: return emptyList()

        val at = nowMillis()
        val leftMake = Asset(leftOrder.make.type, EthUInt256(event.newRightFill()))
        val leftTake = Asset(leftOrder.take.type, EthUInt256(event.newLeftFill()))
        val lestUsdValue = priceUpdateService.getAssetsUsdValue(leftMake, leftTake, at)

        val rightMake = Asset(leftOrder.take.type, EthUInt256(event.newLeftFill()))
        val rightTake = Asset(leftOrder.make.type, EthUInt256(event.newRightFill()))
        val rightUsdValue = priceUpdateService.getAssetsUsdValue(rightMake, rightTake, at)

        val transactionOrders = sideMatchTransactionProvider.getMatchedOrdersByTransactionHash(log.transactionHash())
        val leftMaker = getOriginMaker(event.leftMaker(), transactionOrders?.left?.data)
        val rightMaker = getOriginMaker(event.rightMaker(), transactionOrders?.right?.data)
        val leftAdhoc = transactionOrders?.left?.salt == EthUInt256.ZERO
        val rightAdhoc = transactionOrders?.right?.salt == EthUInt256.ZERO

        return listOf(
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
                date = date,
                data = transactionOrders?.left?.data,
                adhoc = leftAdhoc,
                counterAdhoc = rightAdhoc
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
                date = date,
                data = transactionOrders?.right?.data,
                adhoc = rightAdhoc,
                counterAdhoc = leftAdhoc
            )
        )
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(listOf(exchangeContract))
    }
}


