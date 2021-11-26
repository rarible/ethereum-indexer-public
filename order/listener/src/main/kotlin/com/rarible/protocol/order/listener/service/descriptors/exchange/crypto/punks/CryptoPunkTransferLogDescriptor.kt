package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.protocol.contracts.exchange.crypto.punks.PunkTransferEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks.CryptoPunkBidEnteredLogDescriptor.Companion.getCancelOfPreviousBid
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

/**
 * Listens for PunkTransfer event to support the following case: user 1 makes a bid for a punk of user 2.
 * Then the user 2 transfers the punk for free to the user 1. In this case the bid order of the 1st user must be cancelled.
 */
@Service
class CryptoPunkTransferLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val exchangeHistoryRepository: ExchangeHistoryRepository
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkTransferEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderExchangeHistory> {
        val punkTransferEvent = PunkTransferEvent.apply(log)
        val punkIndex = punkTransferEvent.punkIndex()
        val cancelOfPreviousBid = getCancelOfPreviousBid(exchangeHistoryRepository, log.address(), date, punkIndex)
        if (cancelOfPreviousBid != null && cancelOfPreviousBid.maker == punkTransferEvent.to()) {
            return listOf(cancelOfPreviousBid)
        }
        return emptyList()
    }
}
