package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.auction.v1.event.AuctionFinishedEvent
import com.rarible.protocol.order.core.configuration.EnableAuction
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionFinished
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@EnableAuction
class AuctionFinishedDescriptor(
    contractsProvider: ContractsProvider,
    autoReduceService: AutoReduceService,
) : AbstractAuctionDescriptor<AuctionFinished>(
    name = "auction_finished",
    topic = AuctionFinishedEvent.id(),
    contractsProvider = contractsProvider,
    autoReduceService = autoReduceService,
) {

    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<AuctionFinished> {
        val event = AuctionFinishedEvent.apply(log)
        val contract = log.address()
        val auctionId = EthUInt256.of(event.auctionId())
        val auction = parseContractAuction(event.auction(), timestamp)

        return listOf(
            AuctionFinished(
                seller = auction.seller,
                buyer = auction.buyer,
                sell = auction.sell,
                buy = auction.buy,
                lastBid = auction.lastBid,
                endTime = auction.endTime,
                minimalStep = auction.minimalStep,
                minimalPrice = auction.minimalPrice,
                data = auction.data,
                protocolFee = auction.protocolFee,
                createdAt = timestamp,
                date = timestamp,
                contract = contract,
                auctionId = auctionId,
                hash = Auction.raribleV1HashKey(contract, auctionId),
                source = HistorySource.RARIBLE
            )
        )
    }
}
