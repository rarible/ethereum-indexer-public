package com.rarible.protocol.order.core.service.auction

import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.dto.AuctionActivityEndDto
import com.rarible.protocol.dto.AuctionActivityStartDto
import com.rarible.protocol.order.core.converters.dto.AssetDtoConverter
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.converters.dto.AuctionBidDtoConverter
import com.rarible.protocol.order.core.converters.dto.AuctionDtoConverter
import com.rarible.protocol.order.core.data.randomAuction
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.producer.ProtocolAuctionPublisher
import com.rarible.protocol.order.core.repository.auction.AuctionOffchainHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AuctionStateServiceTest {

    private val primeNormalizer: PriceNormalizer = mockk()

    private val auctionRepository: AuctionRepository = mockk()

    private val assetDtoConverter = AssetDtoConverter(primeNormalizer)
    private val auctionBidDtoConverter = AuctionBidDtoConverter(primeNormalizer)
    private val auctionDtoConverter = AuctionDtoConverter(primeNormalizer, assetDtoConverter, auctionBidDtoConverter)
    private val auctionActivityConverter: AuctionActivityConverter = AuctionActivityConverter(
        auctionDtoConverter,
        auctionBidDtoConverter,
        auctionRepository
    )

    private val auctionOffchainHistoryRepository: AuctionOffchainHistoryRepository = mockk()
    private val eventPublisher: ProtocolAuctionPublisher = mockk()

    private val auctionStateService = AuctionStateService(
        auctionRepository,
        auctionOffchainHistoryRepository,
        eventPublisher,
        auctionActivityConverter
    )

    @BeforeEach
    fun beforeEach() {
        coEvery { auctionOffchainHistoryRepository.save(any()) } returnsArgument 0
        coEvery { eventPublisher.publish(any<AuctionActivityDto>()) } returns Unit
        coEvery { primeNormalizer.normalize(any()) } returns BigDecimal.ZERO
        coEvery { primeNormalizer.normalize(any(), any()) } returns BigDecimal.ZERO
        coEvery { auctionRepository.save(any()) } returnsArgument 0
    }

    @Test
    fun `update ongoing state - started, updated`() = runBlocking {
        // Should be set as ongoing
        val started = randomAuction()

        coEvery { auctionRepository.findById(started.hash) } returns started

        val updated = auctionStateService.updateOngoingState(started.hash, true)
        assertThat(updated).isNotNull
        assertThat(updated!!.ongoing).isTrue()

        auctionStateService.onAuctionOngoingStateUpdated(updated, AuctionOffchainHistory.Type.STARTED)

        coVerify(exactly = 1) { auctionOffchainHistoryRepository.save(any()) }
        coVerify(exactly = 1) {
            auctionOffchainHistoryRepository.save(match {
                it.type == AuctionOffchainHistory.Type.STARTED && it.hash == started.hash
            })
        }

        // Ensure only 1 event was sent
        coVerify(exactly = 1) { eventPublisher.publish(any<AuctionActivityDto>()) }
        coVerify(exactly = 1) {
            eventPublisher.publish(match<AuctionActivityDto> {
                it.auction.hash == started.hash && it is AuctionActivityStartDto
            })
        }
    }

    @Test
    fun `update ongoing state - deleted`() = runBlocking {
        // Deleted for some reason during job execution
        val deleted = randomAuction()

        coEvery { auctionRepository.findById(deleted.hash) } returns null

        val updated = auctionStateService.updateOngoingState(deleted.hash, true)

        assertThat(updated).isNull()
    }

    @Test
    fun `update ongoing state - ended, updated`() = runBlocking {
        // Ended, should be updated
        val ended = randomAuction().copy(ongoing = true)

        coEvery { auctionRepository.findById(ended.hash) } returns ended

        val updated = auctionStateService.updateOngoingState(ended.hash, false)
        assertThat(updated).isNotNull
        assertThat(updated!!.ongoing).isFalse()

        auctionStateService.onAuctionOngoingStateUpdated(updated!!, AuctionOffchainHistory.Type.ENDED)

        coVerify(exactly = 1) { auctionOffchainHistoryRepository.save(any()) }
        coVerify(exactly = 1) {
            auctionOffchainHistoryRepository.save(match {
                it.type == AuctionOffchainHistory.Type.ENDED && it.hash == ended.hash
            })
        }

        // Ensure only 1 event was sent
        coVerify(exactly = 1) { eventPublisher.publish(any<AuctionActivityDto>()) }
        coVerify(exactly = 1) {
            eventPublisher.publish(match<AuctionActivityDto> {
                it.auction.hash == ended.hash && it is AuctionActivityEndDto
            })
        }
    }

    @Test
    fun `update ongoing state - not actual`() = runBlocking {
        // Was updated by Reducer during job execution, already ongoing == true
        val notActual = randomAuction()

        coEvery { auctionRepository.findById(notActual.hash) } returns notActual

        auctionStateService.updateOngoingState(notActual.hash, true)

        // Nothing happened
        coVerify(exactly = 0) { auctionOffchainHistoryRepository.save(any()) }
        coVerify(exactly = 0) { eventPublisher.publish(any<AuctionActivityDto>()) }
    }

}