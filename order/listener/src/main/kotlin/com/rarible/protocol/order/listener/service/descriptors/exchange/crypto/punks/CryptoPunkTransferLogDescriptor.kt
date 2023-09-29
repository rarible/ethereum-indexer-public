package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.protocol.contracts.exchange.crypto.punks.PunkTransferEvent
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks.CryptoPunkBidEnteredLogDescriptor.Companion.getCancelOfPreviousBid
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

/**
 * Listens for PunkTransfer event to support the following case: user 1 makes a bid for a punk of user 2.
 * Then the user 2 transfers the punk for free to the user 1. In this case the bid order of the 1st user must be cancelled.
 */
@Service
class CryptoPunkTransferLogDescriptor(
    contractsProvider: ContractsProvider,
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderExchangeHistory>(
    name = "punk_transfer",
    topic = PunkTransferEvent.id(),
    contracts = contractsProvider.cryptoPunks(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderExchangeHistory> {
        val punkTransferEvent = PunkTransferEvent.apply(log)
        val punkIndex = punkTransferEvent.punkIndex()
        val cancelOfPreviousBid = getCancelOfPreviousBid(
            exchangeHistoryRepository = exchangeHistoryRepository,
            marketAddress = log.address(),
            blockNumber = log.blockNumber().toLong(),
            logIndex = log.logIndex().toInt(),
            blockDate = timestamp,
            punkIndex = punkIndex
        )
        if (cancelOfPreviousBid != null && cancelOfPreviousBid.maker == punkTransferEvent.to()) {
            return listOf(cancelOfPreviousBid)
        }
        return emptyList()
    }
}
