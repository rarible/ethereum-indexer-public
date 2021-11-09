package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.auction.v1.event.BidPlacedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class AuctionBidDescriptor(
    auctionContractAddresses: OrderIndexerProperties.AuctionContractAddresses
) : AbstractAuctionDescriptor<BidPlaced>(auctionContractAddresses) {

    override val topic: Word = BidPlacedEvent.id()

    override fun convert(log: Log, transaction: Transaction, date: Instant): List<BidPlaced> {
        val event = BidPlacedEvent.apply(log)
        val contract = log.address()
        val auctionId = EthUInt256.of(event.auctionId())
        val bid = event.bid().toAuctionBid()
        val endTime = EthUInt256.of(event.endTime())

        return listOf(
            BidPlaced(
                buyer = transaction.from(),
                bid = bid,
                endTime = endTime,
                date = date,
                contract = contract,
                auctionId = auctionId,
                hash = Auction.raribleV1HashKey(contract, auctionId),
                source = HistorySource.RARIBLE
            )
        )
    }
}
