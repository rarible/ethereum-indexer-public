package com.rarible.protocol.order.listener.job

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
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AuctionOngoingUpdateJobTest {

    private val properties = OrderListenerProperties().copy(updateAuctionOngoingStateEnabled = true)
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

    private val job = AuctionOngoingUpdateJob(
        properties,
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
    fun execute() {
        // Should be set as ongoing
        val started = randomAuction()
        // Deleted for some reason during job execution
        val deleted = randomAuction()
        // Ended, should be updated
        val ended = randomAuction().copy(ongoing = true)
        // Was updated by Reducer during job execution
        val notActual = randomAuction()

        coEvery { auctionRepository.findOngoingNotUpdatedIds() } returns listOf(started.hash, deleted.hash).asFlow()
        coEvery { auctionRepository.findEndedNotUpdatedIds() } returns listOf(ended.hash, notActual.hash).asFlow()

        coEvery { auctionRepository.findById(started.hash) } returns started
        coEvery { auctionRepository.findById(deleted.hash) } returns null
        coEvery { auctionRepository.findById(ended.hash) } returns ended
        coEvery { auctionRepository.findById(notActual.hash) } returns notActual

        job.execute()

        // Ensure both history records are saved
        coVerify(exactly = 2) { auctionOffchainHistoryRepository.save(any()) }
        coVerify(exactly = 1) {
            auctionOffchainHistoryRepository.save(match {
                it.type == AuctionOffchainHistory.Type.STARTED && it.hash == started.hash
            })
        }
        coVerify(exactly = 1) {
            auctionOffchainHistoryRepository.save(match {
                it.type == AuctionOffchainHistory.Type.ENDED && it.hash == ended.hash
            })
        }

        // Ensure both activity event are sent
        coVerify(exactly = 2) { eventPublisher.publish(any<AuctionActivityDto>()) }
        coVerify(exactly = 1) {
            eventPublisher.publish(match<AuctionActivityDto> {
                it.auction.hash == started.hash && it is AuctionActivityStartDto
            })
        }
        coVerify(exactly = 1) {
            eventPublisher.publish(match<AuctionActivityDto> {
                it.auction.hash == ended.hash && it is AuctionActivityEndDto
            })
        }
    }
}