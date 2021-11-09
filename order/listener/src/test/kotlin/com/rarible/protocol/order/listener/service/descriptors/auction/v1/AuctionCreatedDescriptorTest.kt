package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.AuctionActivityOpenDto
import com.rarible.protocol.dto.AuctionUpdateEventDto
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
internal class AuctionCreatedDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should create auction`() = runBlocking<Unit> {
        withStartedAuction(userSender1) { (params, chainAuction) ->
            Wait.waitAssert {
                assertThat(chainAuction.seller).isEqualTo(userSender1.from())
                assertThat(chainAuction.buyer).isNull()
                assertThat(chainAuction.endTime).isNotNull()
                assertThat(chainAuction.minimalStep).isEqualTo(params.minimalStep)
                assertThat(chainAuction.minimalPrice).isEqualTo(params.minimalPrice)
                assertThat(chainAuction.sell).isEqualTo(params.sell)
                assertThat(chainAuction.buy).isEqualTo(params.buy)
                assertThat(chainAuction.lastBid).isNull()
                assertThat(chainAuction.data).isEqualTo(params.data)

                val auction = auctionRepository.findById(chainAuction.hash)
                assertThat(auction).isNotNull
                assertThat(auction?.finished).isFalse()
                assertThat(auction?.cancelled).isFalse()
                assertThat(auction?.status).isEqualTo(AuctionStatus.ACTIVE)
            }
            checkAuctionEventWasPublished {
                assertThat(this).isInstanceOfSatisfying(AuctionUpdateEventDto::class.java) {
                    assertThat(it.auctionId).isEqualTo(chainAuction.hash.toString())
                }
            }
            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(AuctionActivityOpenDto::class.java) {
                    assertThat(it.hash).isEqualTo(chainAuction.hash)
                    assertThat(it.seller).isEqualTo(chainAuction.seller)
                }
            }
        }
    }
}

