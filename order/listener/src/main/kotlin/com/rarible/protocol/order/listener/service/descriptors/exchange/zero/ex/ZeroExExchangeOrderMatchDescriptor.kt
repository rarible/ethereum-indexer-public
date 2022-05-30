package com.rarible.protocol.order.listener.service.descriptors.exchange.zero.ex

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.zero.ex.FillEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.zero.ex.ZeroExOrderEventConverter
import com.rarible.protocol.order.listener.service.zero.ex.ZeroExOrderParser
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
class ZeroExExchangeOrderMatchDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val zeroExOrderEventConverter: ZeroExOrderEventConverter,
    private val zeroExOrderParser: ZeroExOrderParser
) : LogEventDescriptor<OrderSideMatch> {

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = FillEvent.id()

    override fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Long,
        index: Int,
        totalLogs: Int
    ): Publisher<OrderSideMatch> {
        return mono {
            convert(
                log,
                transaction,
                Instant.ofEpochSecond(timestamp),
                index,
                totalLogs
            )
        }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(
        log: Log,
        transaction: Transaction,
        date: Instant,
        index: Int,
        totalLogs: Int
    ): List<OrderSideMatch> {
        val event = FillEvent.apply(log)
        val matchOrdersData = zeroExOrderParser.parseMatchOrdersData(
            txHash = transaction.hash(),
            txInput = transaction.input(),
            txFrom = transaction.from(),
            event = event,
            index = index,
            totalLogs = totalLogs
        )
        return zeroExOrderEventConverter.convert(
            matchOrdersData = matchOrdersData,
            from = transaction.from(),
            date = date,
            orderHash = Word.apply(event.orderHash()),
            makerAddress = event.makerAddress(),
            makerAssetFilledAmount = event.makerAssetFilledAmount(),
            takerAssetFilledAmount = event.takerAssetFilledAmount()
        )
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(
        listOf(exchangeContractAddresses.zeroEx)
    )
}
