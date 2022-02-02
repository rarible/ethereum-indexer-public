package com.rarible.protocol.order.listener.job

import com.rarible.protocol.order.core.data.randomAuction
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.core.service.auction.AuctionStateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import org.junit.jupiter.api.Test

class AuctionOngoingUpdateJobTest {

    private val properties = OrderListenerProperties().copy(updateAuctionOngoingStateEnabled = true)

    private val auctionRepository: AuctionRepository = mockk()

    private val auctionStateService: AuctionStateService = mockk()

    private val job = AuctionOngoingUpdateJob(
        properties,
        auctionRepository,
        auctionStateService
    )

    @Test
    fun execute() {
        // Started, updated by service
        val started = randomAuction().copy(ongoing = true)
        // Deleted for some reason during job execution
        val deleted = randomAuction()
        // Ended, updated by service
        val ended = randomAuction().copy(ongoing = false)
        // Was updated by Reducer during job execution
        val notActual = randomAuction()

        coEvery { auctionRepository.findOngoingNotUpdatedIds() } returns listOf(started.hash, deleted.hash).asFlow()
        coEvery { auctionRepository.findEndedNotUpdatedIds(any()) } returns listOf(ended.hash, notActual.hash).asFlow()

        coEvery { auctionStateService.updateOngoingState(started.hash, true) } returns started
        coEvery { auctionStateService.updateOngoingState(deleted.hash, true) } returns null
        coEvery { auctionStateService.updateOngoingState(ended.hash, false) } returns ended
        coEvery { auctionStateService.updateOngoingState(notActual.hash, false) } returns null

        coEvery { auctionStateService.onAuctionOngoingStateUpdated(any(), any()) } returns Unit

        job.execute()

        coVerify(exactly = 1) {
            auctionStateService.onAuctionOngoingStateUpdated(ended, AuctionOffchainHistory.Type.ENDED)
        }
        coVerify(exactly = 1) {
            auctionStateService.onAuctionOngoingStateUpdated(started, AuctionOffchainHistory.Type.STARTED)
        }

    }
}