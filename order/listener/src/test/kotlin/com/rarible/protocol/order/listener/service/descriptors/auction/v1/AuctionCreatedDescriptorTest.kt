package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
internal class AuctionCreatedDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should create auction`() = runBlocking<Unit> {
        withStartedAuction(userSender1) { startedActions ->
            Wait.waitAssert {
                val events = auctionHistoryRepository.findByType(AuctionHistoryType.ON_CHAIN_AUCTION).collectList().awaitFirst()
                assertThat(events).hasSize(1)

                val chainAuction = events.map { event -> event.data as OnChainAuction }.single()
                assertThat(chainAuction.seller).isEqualTo(startedActions.seller)
                assertThat(chainAuction.buyer).isNull()
                assertThat(chainAuction.endTime).isNotNull()
                assertThat(chainAuction.minimalStep).isEqualTo(startedActions.minimalStep)
                assertThat(chainAuction.minimalPrice).isEqualTo(startedActions.minimalPrice)
                assertThat(chainAuction.sell).isEqualTo(startedActions.sell)
                assertThat(chainAuction.buy).isEqualTo(startedActions.buy)
                assertThat(chainAuction.lastBid).isNull()
                assertThat(chainAuction.data).isEqualTo(startedActions.data)
            }
        }
    }
}

