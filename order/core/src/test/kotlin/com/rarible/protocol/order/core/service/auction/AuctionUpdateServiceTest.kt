package com.rarible.protocol.order.core.service.auction

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.data.randomAuction
import com.rarible.protocol.order.core.event.AuctionListener
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuctionUpdateServiceTest {

    private val auctionRepository: AuctionRepository = mockk()
    private val auctionListener: AuctionListener = mockk()
    private val auctionStateService: AuctionStateService = mockk()

    private val auctionUpdateService = AuctionUpdateService(
        auctionRepository,
        listOf(auctionListener),
        auctionStateService
    )

    @BeforeEach
    fun beforeEach() {
        coEvery { auctionListener.onAuctionUpdate(any()) } returns Unit
        coEvery { auctionListener.onAuctionDelete(any()) } returns Unit
        coEvery { auctionRepository.save(any()) } returnsArgument 0
    }

    @Test
    fun `update first time`() = runBlocking<Unit> {
        val auction = randomAuction()

        // Auction doesn't exist in DB
        coEvery { auctionRepository.findById(auction.hash) } returns null

        auctionUpdateService.update(auction)

        coVerify(exactly = 1) { auctionRepository.save(auction.withCalculatedState()) }
        coVerify(exactly = 1) { auctionListener.onAuctionUpdate(auction) }
    }

    @Test
    fun `update existing auction`() = runBlocking<Unit> {
        val auction = randomAuction()
        val existing = auction.copy(version = 10, ongoing = true, endTime = nowMillis())

        // Auction doesn't exist in DB
        coEvery { auctionRepository.findById(auction.hash) } returns existing

        auctionUpdateService.update(auction)

        coVerify(exactly = 1) {
            auctionRepository.save(match {
                it.version == 10L && it.ongoing // Version and 'ongoing was NOT changed'
                        && it.endTime == auction.endTime // But other fields applied from received auction
            })
        }
        coVerify(exactly = 1) { auctionListener.onAuctionUpdate(auction.copy(version = 10, ongoing = true)) }
    }

    @Test
    fun `update existing auction - ongoing reset`() = runBlocking<Unit> {
        val auction = randomAuction().copy(cancelled = true, ongoing = true)
        val existing = auction.copy(ongoing = true)
        val expected = auction.copy(ongoing = false, status = AuctionStatus.CANCELLED)

        // Auction doesn't exist in DB
        coEvery { auctionRepository.findById(auction.hash) } returns existing

        auctionUpdateService.update(auction)

        coVerify(exactly = 1) {
            auctionRepository.save(match {
                !it.ongoing // Ongoing flag should be reset since auction is not active anymore
            })
        }
        coVerify(exactly = 1) { auctionListener.onAuctionUpdate(expected) }
    }

    @Test
    fun `update existing auction - ongoing auction finished`() = runBlocking<Unit> {
        val auction = randomAuction().copy(finished = true, ongoing = true, status = AuctionStatus.FINISHED)
        val existing = auction.copy(ongoing = true, status = AuctionStatus.ACTIVE)
        val expected = auction.copy(ongoing = false)

        // Auction doesn't exist in DB
        coEvery { auctionRepository.findById(auction.hash) } returns existing
        coEvery { auctionStateService.onAuctionOngoingStateUpdated(any(), any()) } returns Unit

        auctionUpdateService.update(auction)

        coVerify(exactly = 1) {
            auctionRepository.save(match {
                !it.ongoing // Ongoing flag should be reset since auction is not active anymore
            })
        }
        coVerify(exactly = 1) {
            auctionStateService.onAuctionOngoingStateUpdated(expected, AuctionOffchainHistory.Type.ENDED)
        }
        coVerify(exactly = 1) { auctionListener.onAuctionUpdate(expected) }
    }

    @Test
    fun `delete existing auction`() = runBlocking<Unit> {
        val auction = randomAuction().copy(deleted = true)

        coEvery { auctionRepository.remove(auction.hash) } returns Unit

        auctionUpdateService.update(auction)

        coVerify(exactly = 1) { auctionRepository.remove(auction.hash) }
        coVerify(exactly = 1) { auctionListener.onAuctionDelete(auction) }
    }

}