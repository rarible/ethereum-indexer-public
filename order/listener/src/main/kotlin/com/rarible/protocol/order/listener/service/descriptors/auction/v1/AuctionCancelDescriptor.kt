package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.auction.v1.event.AuctionCancelledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import java.time.Instant

@Service
class AuctionCancelDescriptor(
    auctionContractAddresses: OrderIndexerProperties.AuctionContractAddresses
) : AbstractAuctionDescriptor<AuctionCancelled>(auctionContractAddresses) {

    override val topic: Word = AuctionCancelledEvent.id()

    override fun convert(log: Log, date: Instant): List<AuctionCancelled> {
        val event = AuctionCancelledEvent.apply(log)
        val contract = log.address()
        val auctionId = EthUInt256.of(event.auctionId())

        return listOf(
            AuctionCancelled(
                auctionId = auctionId,
                date = date,
                contract = contract,
                hash = Auction.raribleV1HashKey(contract, auctionId)
            )
        )
    }
}
