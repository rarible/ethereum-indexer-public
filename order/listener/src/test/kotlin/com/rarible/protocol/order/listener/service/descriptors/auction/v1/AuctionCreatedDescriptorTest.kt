package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
internal class AuctionCreatedDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should create auction`() = runBlocking<Unit> {
        withStartedAuction(userSender1) { (params, chainAuction) ->
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
        }
    }
}

