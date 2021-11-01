package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

@FlowPreview
@IntegrationTest
internal class AuctionBidDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should put bid`() = runBlocking<Unit> {
        withStartedAuction(seller = userSender1, startTime = EthUInt256.ZERO) { (_, chainAuction) ->
            val bid = BidV1(
                amount = EthUInt256.ONE,
                data = BidDataV1(
                    originFees = listOf(
                        Part(randomAddress(), EthUInt256.of(5000)),
                        Part(randomAddress(), EthUInt256.of(5000))
                    ),
                    payouts = listOf(
                        Part(randomAddress(), EthUInt256.of(5000)),
                        Part(randomAddress(), EthUInt256.of(5000))
                    )
                )
            )
            depositInitialBalance(userSender2.from(), BigInteger.TEN.pow(18))
            auctionHouse.putBid(chainAuction.auctionId.value, bid.forTx()).withSender(userSender2).execute().verifySuccess()

            Wait.waitAssert {
                val events = auctionHistoryRepository.findByType(AuctionHistoryType.BID_PLACED).collectList().awaitFirst()
                Assertions.assertThat(events).hasSize(1)

                val bidEvent = events.map { event -> event.data as BidPlaced }.single()
                Assertions.assertThat(bidEvent.auctionId).isEqualTo(chainAuction.auctionId)
                Assertions.assertThat(bidEvent.bid).isEqualTo(bid)
                Assertions.assertThat(bidEvent.buyer).isEqualTo(userSender2.from())

                val auction = auctionRepository.findById(chainAuction.hash)
                Assertions.assertThat(auction).isNotNull
                Assertions.assertThat(auction?.finished).isFalse()
                Assertions.assertThat(auction?.cancelled).isFalse()
            }
        }
    }
}
