package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.auction.v1.event.BidPlacedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class AuctionBidDescriptor(
    auctionContractAddresses: OrderIndexerProperties.AuctionContractAddresses,
    private val prizeNormalizer: PriceNormalizer,
    private val auctionRepository: AuctionRepository
) : AbstractAuctionDescriptor<BidPlaced>(auctionContractAddresses) {

    override val topic: Word = BidPlacedEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<BidPlaced> {
        val event = BidPlacedEvent.apply(log)
        val buyer = event.buyer()
        val contract = log.address()
        val auctionId = EthUInt256.of(event.auctionId())
        val bid = toBid(event.bid(), date)
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
                date = date,
                contract = contract,
                auctionId = auctionId,
                hash = hash,
                source = HistorySource.RARIBLE
            )
        )
    }
}
