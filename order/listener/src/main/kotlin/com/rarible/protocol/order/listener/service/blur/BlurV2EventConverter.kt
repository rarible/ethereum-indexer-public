package com.rarible.protocol.order.listener.service.blur

import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.blur.exchange.v2.BlurExchangeV2
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.BlurV2ExecutionEvent
import com.rarible.protocol.order.core.model.BlurV2Order
import com.rarible.protocol.order.core.model.BlurV2OrderType
import com.rarible.protocol.order.core.model.BlurV2Take
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.parser.BlurV2Parser
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.converter.AbstractEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.time.Instant

@Component
class BlurV2EventConverter(
    traceCallService: TraceCallService,
    featureFlags: OrderIndexerProperties.FeatureFlags,
    private val contractsProvider: ContractsProvider,
    private val prizeNormalizer: PriceNormalizer,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val sender: MonoTransactionSender,
) : AbstractEventConverter(traceCallService, featureFlags) {

    suspend fun convertBlurV2ExecutionEvent(
        log: Log,
        transaction: Transaction,
        index: Int,
        totalLogs: Int,
        date: Instant,
        converter: (Log) -> BlurV2ExecutionEvent,
    ): List<OrderSideMatch> {
        val blurV2Takes = getTakes(log, transaction)
        val blurV2Take = blurV2Takes.single()

        val orders = blurV2Take.orders
        val executionEvent = converter(log)

        val hash = Word.apply(executionEvent.orderHash)
        val counterHash = keccak256(hash)

        val order = if (orders.size == 1) {
            orders.first()
        } else {
            findOrder(orders, hash, executionEvent.orderType) ?: run {
                logger.error("Can't find blur order for batch execution: tx=${transaction.hash()}")
                return emptyList()
            }
        }
        val (make, take) = when (executionEvent.orderType) {
            BlurV2OrderType.ASK -> executionEvent.nft() to executionEvent.ethPayment()
            BlurV2OrderType.BID -> executionEvent.erc20Payment(contractsProvider.weth()) to executionEvent.nft()
        }
        val taker = when (executionEvent.orderType) {
            BlurV2OrderType.ASK -> blurV2Take.tokenRecipient
            BlurV2OrderType.BID -> transaction.from()
        }
        val makeOriginFees = listOfNotNull(
            executionEvent.makerFee,
        ).map {
            Part(it.recipient, EthUInt256.of(it.rate))
        }
        val takeOriginFees = listOfNotNull(
            executionEvent.takerFee,
        ).map {
            Part(it.recipient, EthUInt256.of(it.rate))
        }
        val left = OrderSideMatch(
            hash = hash,
            counterHash = counterHash,
            maker = order.trader,
            taker = taker,
            side = OrderSide.LEFT,
            make = make,
            take = take,
            fill = make.value,
            makeValue = prizeNormalizer.normalize(make),
            takeValue = prizeNormalizer.normalize(take),
            adhoc = false,
            source = HistorySource.BLUR,
            date = date,
            originFees = makeOriginFees,
            makeUsd = null,
            takeUsd = null,
            makePriceUsd = null,
            takePriceUsd = null,
            counterAdhoc = true,
            origin = null,
            externalOrderExecutedOnRarible = null,
        )
        val right = OrderSideMatch(
            hash = counterHash,
            counterHash = hash,
            maker = left.taker,
            taker = left.maker,
            side = OrderSide.RIGHT,
            make = left.take,
            take = left.make,
            fill = take.value,
            makeValue = prizeNormalizer.normalize(left.take),
            takeValue = prizeNormalizer.normalize(left.make),
            source = HistorySource.BLUR,
            counterAdhoc = false,
            date = date,
            originFees = takeOriginFees,
            makeUsd = null,
            takeUsd = null,
            makePriceUsd = null,
            takePriceUsd = null,
            adhoc = true,
            origin = null,
            externalOrderExecutedOnRarible = null,
        )
        return OrderSideMatch.addMarketplaceMarker(listOf(left, right), transaction.input())
    }

    private suspend fun getTakes(log: Log, transaction: Transaction): List<BlurV2Take> {
        val txHash = transaction.hash()
        val txInput = transaction.input()

        return if (txInput.methodSignatureId() in blurV2ExchangeMethodIds) {
            listOf(parserBlurV2(txInput, txHash))
        } else {
            emptyList()
        }.ifEmpty {
            getMethodInput(
                log,
                transaction,
                *blurV2ExchangeMethodIds.toTypedArray()
            ).map { parserBlurV2(it, txHash) }
        }
    }

    private suspend fun findOrder(orders: List<BlurV2Order>, hash: Word, type: BlurV2OrderType): BlurV2Order? {
        val market = BlurExchangeV2(contractsProvider.blurV2().single(), sender)
        val groupedOrder = orders.groupBy { it.trader }
        groupedOrder.forEach { (trader, orders) ->
            val nonce = market.nonces(trader).execute().awaitSingle().toBigInteger()
            val found = findOrder(orders, type, hash, nonce)
            if (found != null) return found
        }
        return null
    }

    private suspend fun findOrder(
        orders: List<BlurV2Order>,
        type: BlurV2OrderType,
        hash: Word,
        maxNonce: BigInteger
    ): BlurV2Order? {
        for (order in orders) {
            for (nonce in (maxNonce.longValueExact() downTo 0)) {
                val calculatedHash = Order.blurV2Hash(order, type, nonce.toBigInteger())
                if (hash == calculatedHash) return order
            }
        }
        return null
    }

    private fun parserBlurV2(input: Binary, tx: Word) = BlurV2Parser.parserBlurV2(input, tx)

    private val blurV2ExchangeMethodIds = setOf(
        BlurExchangeV2.takeAskSignature().id(),
        BlurExchangeV2.takeAskSingleSignature().id(),

        BlurExchangeV2.takeBidSignature().id(),
        BlurExchangeV2.takeBidSingleSignature().id(),

        BlurExchangeV2.takeAskPoolSignature().id(),
        BlurExchangeV2.takeAskSinglePoolSignature().id()
    )
}
