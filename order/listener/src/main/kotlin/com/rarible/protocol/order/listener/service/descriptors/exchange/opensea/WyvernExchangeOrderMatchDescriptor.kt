package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class WyvernExchangeOrderMatchDescriptor(
    exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val openSeaOrdersSideMatcher: OpenSeaOrderEventConverter,
    private val openSeaOrderParser: OpenSeaOrderParser
) : LogEventDescriptor<OrderSideMatch> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val exchangeContract = exchangeContractAddresses.openSeaV1

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = OrdersMatchedEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long): Publisher<OrderSideMatch> {
        return mono { convert(log, transaction, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderSideMatch> {
        val transactionHash =  log.transactionHash()
        logger.info("Got OrdersMatchedEvent, tx=$transactionHash")

        val orders = openSeaOrderParser.parseMatchedOrders(transaction.input().prefixed())
        val event = OrdersMatchedEvent.apply(log)
        return openSeaOrdersSideMatcher.convert(orders, event.price(), date)
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(listOf(exchangeContract))
    }
}


