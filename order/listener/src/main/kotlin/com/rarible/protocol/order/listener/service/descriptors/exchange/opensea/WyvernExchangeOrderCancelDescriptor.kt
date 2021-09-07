package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderProvider
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderEventConverter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
class WyvernExchangeOrderCancelDescriptor(
    exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val openSeaOrderProvider: OpenSeaOrderProvider,
    private val openSeaOrderEventConverter: OpenSeaOrderEventConverter
) : LogEventDescriptor<OrderCancel> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val exchangeContract = exchangeContractAddresses.openSeaV1

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = OrderCancelledEvent.id()

    override fun convert(log: Log, timestamp: Long): Publisher<OrderCancel> {
        return mono { convert(log, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, date: Instant): List<OrderCancel> {
        val transactionHash =  log.transactionHash()
        logger.info("Got OrderCancel event, tx=$transactionHash")

        val order = openSeaOrderProvider.getCancelOrderByTransactionHash(transactionHash)

        return if (order != null) {
            openSeaOrderEventConverter.convert(order, date)
        } else {
            emptyList()
        }
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(listOf(exchangeContract))
    }
}
