package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.v2.events.CancelEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
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
class ExchangeV2CancelDescriptor(
    exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses
) : LogEventDescriptor<OrderCancel> {

    private val exchangeContract = exchangeContractAddresses.v2

    override val collection: String = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = CancelEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long): Publisher<OrderCancel> {
        return mono { listOfNotNull(convert(log, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(setOf(exchangeContract))

    private fun convert(log: Log, date: Instant): OrderCancel {
        val event = CancelEvent.apply(log)
        val hash = Word.apply(event.hash())
        val makeType = event.makeAssetType().toAssetType()
        val takeType = event.takeAssetType().toAssetType()

        return OrderCancel(
            hash = hash,
            make = Asset(makeType, EthUInt256.ZERO),
            take = Asset(takeType, EthUInt256.ZERO),
            date = date,
            maker = event.maker(),
            source = HistorySource.RARIBLE
        )
    }
}
