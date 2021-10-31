package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.auction.v1.event.AuctionFinishedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import java.time.Instant

@Service
class AuctionFinishedDescriptor(
    auctionContractAddresses: OrderIndexerProperties.AuctionContractAddresses
) : AbstractAuctionDescriptor<AuctionFinished>(auctionContractAddresses) {

    override val topic: Word = AuctionFinishedEvent.id()

    override fun convert(log: Log, date: Instant): List<AuctionFinished> {
        val event = AuctionFinishedEvent.apply(log)
        val contract = log.address()
        val auctionId = EthUInt256.of(event.auctionId())
        val auction = parseContractAuction(event.auction())

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
                createdAt = date,
                date = date,
                contract = contract,
                auctionId = auctionId,
                hash = Auction.raribleV1HashKey(contract, auctionId)
            )
        )
    }
}
