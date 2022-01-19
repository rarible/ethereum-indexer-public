package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.AuctionActivityOpenDto
import com.rarible.protocol.dto.AuctionUpdateEventDto
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
internal class AuctionCreatedDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should create auction`() = runBlocking<Unit> {
        withStartedAuction(userSender1) { (params, chainAuction) ->
            Wait.waitAssert {
                val expectedChainAuction = params.toExpectedOnChainAuction(
                    createdAt = chainAuction.createdAt,
                    endTime = chainAuction.endTime
                )
                assertThat(expectedChainAuction).isEqualTo(chainAuction)

                val auction = auctionRepository.findById(chainAuction.hash)
                assertThat(auction).isNotNull

                val expectedAuction = params.toExpectedAuction(
                    auctionStatus = AuctionStatus.ACTIVE,
                    cancelled = false,
                    finished = false,
                    createdAt = auction!!.createdAt,
                    endTime = auction.endTime,
                    startTime = auction.startTime,
                    lastEventId = auction.lastEventId
                )
                assertThat(auction.copy(version = null)).isEqualTo(expectedAuction)
            }
            checkAuctionEventWasPublished {
                assertThat(this).isInstanceOfSatisfying(AuctionUpdateEventDto::class.java) {
                    assertThat(it.auctionId).isEqualTo(chainAuction.hash.toString())
                }
            }
            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(AuctionActivityOpenDto::class.java) {
                    assertThat(it.auction.hash).isEqualTo(chainAuction.hash)
                    assertThat(it.auction.seller).isEqualTo(chainAuction.seller)
                }
            }
        }
    }
}
