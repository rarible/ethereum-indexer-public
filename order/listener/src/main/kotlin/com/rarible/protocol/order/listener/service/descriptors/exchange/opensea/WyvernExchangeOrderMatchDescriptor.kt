package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.wyvern.OrdersMatchedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderEventConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderParser
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
class WyvernExchangeOrderMatchDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val openSeaOrdersSideMatcher: OpenSeaOrderEventConverter,
    private val openSeaOrderParser: OpenSeaOrderParser
) : LogEventDescriptor<OrderSideMatch> {

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = OrdersMatchedEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<OrderSideMatch> {
        return mono { convert(log, transaction, Instant.ofEpochSecond(timestamp), index, totalLogs) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, date: Instant, index: Int, totalLogs: Int): List<OrderSideMatch> {
        val event = OrdersMatchedEvent.apply(log)
        val eip712 = log.address() == exchangeContractAddresses.openSeaV2

        val orders= openSeaOrderParser.parseMatchedOrders(
            txHash = transaction.hash(),
            txInput = transaction.input(),
            event = event,
            index = index,
            totalLogs = totalLogs,
            eip712 = eip712
        )
        return if (orders != null) {
            openSeaOrdersSideMatcher.convert(
                openSeaOrders = orders,
                from = transaction.from(),
                price = event.price(),
                date = date,
                input = transaction.input()
            )
        } else {
            emptyList()
        }
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(
        listOf(exchangeContractAddresses.openSeaV1, exchangeContractAddresses.openSeaV2)
    )
}
