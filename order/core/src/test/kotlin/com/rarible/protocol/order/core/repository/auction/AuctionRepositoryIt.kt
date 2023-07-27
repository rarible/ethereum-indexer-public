package com.rarible.protocol.order.core.repository.auction

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.data.randomAuction
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.AuctionStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
class AuctionRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var auctionRepository: AuctionRepository

    @Test
    fun `find ongoing but not updated`() = runBlocking<Unit> {
        auctionRepository.createIndexes()
        val now = nowMillis()
        val before = now.minusSeconds(60)
        val after = now.plusSeconds(60)

        // candidates to set ongoing = true
        val ongoing = auctionRepository.save(randomAuction().copy(startTime = before, endTime = after))
        val withoutStart = auctionRepository.save(randomAuction().copy(startTime = null, endTime = after))
        val withoutStartEnd = auctionRepository.save(randomAuction().copy(startTime = null, endTime = null))
        val withoutEnd = auctionRepository.save(randomAuction().copy(startTime = before, endTime = null))

        // already ongoing, should not be returned
        auctionRepository.save(randomAuction().copy(startTime = null, endTime = null, ongoing = true))

        // not ongoing
        auctionRepository.save(randomAuction().copy(startTime = after, endTime = null))
        auctionRepository.save(randomAuction().copy(startTime = null, endTime = before))

        // not active
        auctionRepository.save(randomAuction().copy(startTime = null, endTime = after, status = AuctionStatus.FINISHED))
        auctionRepository.save(randomAuction().copy(startTime = null, endTime = null, status = AuctionStatus.CANCELLED))

        val ongoingNotUpdated = auctionRepository.findOngoingNotUpdatedIds().toList()

        assertThat(ongoingNotUpdated).contains(ongoing.hash)
        assertThat(ongoingNotUpdated).contains(withoutStart.hash)
        assertThat(ongoingNotUpdated).contains(withoutStartEnd.hash)
        assertThat(ongoingNotUpdated).contains(withoutEnd.hash)

        assertThat(ongoingNotUpdated).hasSize(4)
    }

    @Test
    fun `find ended but not updated`() = runBlocking<Unit> {
        auctionRepository.createIndexes()
        val now = nowMillis()
        val before = now.minusSeconds(60)
        val after = now.plusSeconds(60)
        val afterWithLag = now.plusSeconds(360)

        // candidates to set ongoing = false
        val endedRecently = auctionRepository.save(
            randomAuction().copy(startTime = before, endTime = after, ongoing = true)
        )
        val ended = auctionRepository.save(
            randomAuction().copy(startTime = before, endTime = afterWithLag, ongoing = true)
        )

        // already updated, should not be returned
        auctionRepository.save(randomAuction().copy(startTime = null, endTime = after, ongoing = false))

        // not active (originally there should not be such records with status != ACTIVE and ongoing = true)
        auctionRepository.save(
            randomAuction().copy(
                startTime = null, endTime = after, status = AuctionStatus.FINISHED, ongoing = true
            )
        )
        auctionRepository.save(
            randomAuction().copy(
                startTime = null, endTime = null, status = AuctionStatus.CANCELLED, ongoing = true
            )
        )

        val lag = Duration.ofMinutes(5)
        val ongoingNotUpdated = auctionRepository.findEndedNotUpdatedIds(lag).toList()

        assertThat(ongoingNotUpdated).contains(ended.hash)
        assertThat(ongoingNotUpdated).hasSize(1)
    }
}
