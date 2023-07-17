package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.AuctionActivityCancelDto
import com.rarible.protocol.dto.AuctionUpdateEventDto
import com.rarible.protocol.order.core.model.AuctionCancelled
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@IntegrationTest
@Disabled("Rewrite after auction resurrection")
internal class AuctionCancelDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should cancel auction`() = runBlocking<Unit> {
        withStartedAuction(userSender1) { (_, chainAuction) ->
            auctionHouse.cancel(chainAuction.auctionId.value).withSender(userSender1).execute().verifySuccess()

            Wait.waitAssert {
                val events = auctionHistoryRepository.findByType(AuctionHistoryType.AUCTION_CANCELLED).collectList().awaitFirst()
                Assertions.assertThat(events).hasSize(1)

                val cancelEvent = events.map { event -> event.data as AuctionCancelled }.single()
                Assertions.assertThat(cancelEvent.auctionId).isEqualTo(chainAuction.auctionId)

                val auction = auctionRepository.findById(chainAuction.hash)
                Assertions.assertThat(auction).isNotNull
                Assertions.assertThat(auction?.finished).isTrue()
                Assertions.assertThat(auction?.cancelled).isTrue()
                Assertions.assertThat(auction?.status).isEqualTo(AuctionStatus.CANCELLED)
            }
            checkAuctionEventWasPublished {
                Assertions.assertThat(this).isInstanceOfSatisfying(AuctionUpdateEventDto::class.java) {
                    Assertions.assertThat(it.auctionId).isEqualTo(chainAuction.hash.toString())
                }
            }
            checkActivityWasPublished {
                Assertions.assertThat(this).isInstanceOfSatisfying(AuctionActivityCancelDto::class.java) {
                    Assertions.assertThat(it.auction.hash).isEqualTo(chainAuction.hash)
                }
            }
        }
    }
}
