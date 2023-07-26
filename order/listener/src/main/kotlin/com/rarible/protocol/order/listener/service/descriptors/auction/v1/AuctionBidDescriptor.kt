package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.auction.v1.event.BidPlacedEvent
import com.rarible.protocol.order.core.configuration.EnableAuction
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.BidPlaced
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@EnableAuction
class AuctionBidDescriptor(
    contractsProvider: ContractsProvider,
    private val prizeNormalizer: PriceNormalizer,
    private val auctionRepository: AuctionRepository
) : AbstractAuctionDescriptor<BidPlaced>(
    name = "auction_bid_placed",
    topic = BidPlacedEvent.id(),
    contractsProvider = contractsProvider
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<BidPlaced> {
        val event = BidPlacedEvent.apply(log)
        val buyer = event.buyer()
        val contract = log.address()
        val auctionId = EthUInt256.of(event.auctionId())
        val bid = toBid(event.bid(), timestamp)
        val endTime = EthUInt256.of(event.endTime())
        val hash = Auction.raribleV1HashKey(contract, auctionId)
        val auction = auctionRepository.findById(hash)
        val bidValue = auction?.let { prizeNormalizer.normalize(auction.buy, bid.amount.value) }

        return listOf(
            BidPlaced(
                buyer = buyer,
                bid = bid,
                bidValue = bidValue,
                sell = auction?.sell,
                endTime = endTime,
                date = timestamp,
                contract = contract,
                auctionId = auctionId,
                hash = hash,
                source = HistorySource.RARIBLE
            )
        )
    }
}
