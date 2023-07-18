package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.AuctionActivityFinishDto
import com.rarible.protocol.dto.AuctionUpdateEventDto
import com.rarible.protocol.order.core.model.AuctionFinished
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant

@IntegrationTest
@Disabled("Rewrite after auction resurrection")
internal class AuctionFinishedDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should finish auction`() = runBlocking<Unit> {
        //0x0000000000000000000000000000000000000000
        withStartedAuction(seller = userSender1, startTime = Instant.EPOCH) { (_, chainAuction) ->
            auctionHouse.cancel(chainAuction.auctionId.value).withSender(userSender1).execute().verifySuccess()

            Wait.waitAssert {
                val events =
                    auctionHistoryRepository.findByType(AuctionHistoryType.AUCTION_FINISHED).collectList().awaitFirst()
                assertThat(events).hasSize(1)

                val finishEvent = events.map { event -> event.data as AuctionFinished }.single()
                assertThat(finishEvent.auctionId).isEqualTo(chainAuction.auctionId)
                assertThat(finishEvent.hash).isEqualTo(chainAuction.hash)

                val auction = auctionRepository.findById(chainAuction.hash)
                assertThat(auction).isNotNull
                assertThat(auction?.finished).isTrue()
                assertThat(auction?.cancelled).isTrue()
            }
            checkAuctionEventWasPublished {
                assertThat(this).isInstanceOfSatisfying(AuctionUpdateEventDto::class.java) {
                    assertThat(it.auctionId).isEqualTo(chainAuction.hash.toString())
                }
            }
            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(AuctionActivityFinishDto::class.java) {
                    assertThat(it.auction.hash).isEqualTo(chainAuction.hash)
                }
            }
        }
    }
}
