package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.data.randomAuction
import com.rarible.protocol.order.listener.data.randomAuctionV1DataV1
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

@FlowPreview
@IntegrationTest
internal class AuctionCreatedDescriptorTest : AbstractAuctionDescriptorTest() {
    @Test
    fun `should create auction`() = runBlocking<Unit> {
        val seller = userSender1.from()
        val erc721AssetType = mintErc721(seller)

        val adhocAuction = randomAuction().copy(
            seller = seller,
            sell = Asset(erc721AssetType, EthUInt256.ONE),
            buy = EthAssetType,
            contract = auctionHouse.address(),
            data = randomAuctionV1DataV1().copy(
                duration = Duration.ofHours(1).let { EthUInt256.of(it.seconds) }
            )
        )
        adhocAuction.forTx().let { forTx ->
            auctionHouse.startAuction(
                forTx._1(),
                forTx._2(),
                forTx._3(),
                forTx._4(),
                forTx._5(),
                forTx._6()
            ).withSender(userSender1).execute().verifySuccess()
        }
        Wait.waitAssert {
            val events = auctionHistoryRepository.findLogEvents(hash = adhocAuction.hash).collectList().awaitFirst()
            assertThat(events).hasSize(1)

//            val auction = auctionRepository.findById(adhocAuction.hash)
//            assertThat(auction).isNotNull
        }
    }
}

