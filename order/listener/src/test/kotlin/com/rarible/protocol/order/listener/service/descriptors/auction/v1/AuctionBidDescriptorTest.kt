package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AuctionActivityBidDto
import com.rarible.protocol.dto.AuctionUpdateEventDto
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.core.model.BidDataV1
import com.rarible.protocol.order.core.model.BidPlaced
import com.rarible.protocol.order.core.model.BidV1
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit

@IntegrationTest
internal class AuctionBidDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should put bid`() = runBlocking<Unit> {
        withStartedAuction(seller = userSender1, startTime = Instant.EPOCH) { (_, chainAuction) ->
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
                ),
                date = now().truncatedTo(ChronoUnit.SECONDS)
            )
            depositInitialBalance(userSender2.from(), BigInteger.TEN.pow(18))
            auctionHouse.putBid(chainAuction.auctionId.value, bid.forTx()).withSender(userSender2).execute().verifySuccess()

            Wait.waitAssert {
                val events = auctionHistoryRepository.findByType(AuctionHistoryType.BID_PLACED).collectList()
                    .awaitFirst()
                Assertions.assertThat(events).hasSize(1)

                val bidEvent = events.map { event -> event.data as BidPlaced }.single()
                Assertions.assertThat(bidEvent.auctionId).isEqualTo(chainAuction.auctionId)
                // Date could be a bit different - depends on time of block
                Assertions.assertThat(bidEvent.bid).isEqualTo(bid.copy(date = bidEvent.date))
                Assertions.assertThat(bidEvent.buyer).isEqualTo(userSender2.from())

                val auction = auctionRepository.findById(chainAuction.hash)
                Assertions.assertThat(auction).isNotNull
                Assertions.assertThat(auction?.finished).isFalse()
                Assertions.assertThat(auction?.cancelled).isFalse()
                Assertions.assertThat(auction?.status).isEqualTo(AuctionStatus.ACTIVE)
            }
            checkAuctionEventWasPublished {
                Assertions.assertThat(this).isInstanceOfSatisfying(AuctionUpdateEventDto::class.java) {
                    Assertions.assertThat(it.auctionId).isEqualTo(chainAuction.hash.toString())
                }
            }
            checkActivityWasPublished {
                Assertions.assertThat(this).isInstanceOfSatisfying(AuctionActivityBidDto::class.java) {
                    Assertions.assertThat(it.auction.hash).isEqualTo(chainAuction.hash)
                }
            }
        }
    }
}
