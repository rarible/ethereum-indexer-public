package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.auction.v1.event.AuctionCancelledEvent
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.listener.service.descriptors.ContractsProvider
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class AuctionCancelDescriptor(
    contractsProvider: ContractsProvider,
    private val auctionRepository: AuctionRepository
) : AbstractAuctionDescriptor<AuctionCancelled>(AuctionCancelledEvent.id(), contractsProvider) {

    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<AuctionCancelled> {
        val event = AuctionCancelledEvent.apply(log)
        val contract = log.address()
        val auctionId = EthUInt256.of(event.auctionId())
        val auction = auctionRepository.findById(Auction.raribleV1HashKey(contract, auctionId))
        return listOf(
            AuctionCancelled(
                auctionId = auctionId,
                seller = auction?.seller,
                sell = auction?.sell,
                date = timestamp,
                contract = contract,
                hash = Auction.raribleV1HashKey(contract, auctionId),
                source = HistorySource.RARIBLE
            )
        )
    }
}
