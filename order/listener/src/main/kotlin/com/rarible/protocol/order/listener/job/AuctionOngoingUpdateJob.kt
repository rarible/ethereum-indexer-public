package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.CaptureTransaction
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.producer.ProtocolAuctionPublisher
import com.rarible.protocol.order.core.repository.auction.AuctionOffchainHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AuctionOngoingUpdateJob(
    private val properties: OrderListenerProperties,
    private val auctionRepository: AuctionRepository,
    private val auctionOffchainHistoryRepository: AuctionOffchainHistoryRepository,
    private val eventPublisher: ProtocolAuctionPublisher,
    private val auctionActivityConverter: AuctionActivityConverter
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.updateAuctionOngoingState}")
    @CaptureTransaction(value = "auction_ongoing_update")
    fun execute() = runBlocking<Unit> {
        if (properties.updateAuctionOngoingStateEnabled.not()) return@runBlocking

        auctionRepository.findOngoingNotUpdatedIds().collect {
            val auction = updateOngoing(it, true)
            onAuctionUpdated(auction, AuctionOffchainHistory.Type.STARTED)
        }
        auctionRepository.findEndedNotUpdatedIds().collect {
            val auction = updateOngoing(it, false)
            onAuctionUpdated(auction, AuctionOffchainHistory.Type.ENDED)
        }
    }

    private suspend fun updateOngoing(hash: Word, ongoing: Boolean): Auction? {
        return optimisticLock {
            val auction = auctionRepository.findById(hash)
            auction?.let {
                if (auction.ongoing != ongoing) {
                    // Auction updated - return updated entity
                    auctionRepository.save(auction.copy(ongoing = ongoing))
                } else {
                    logger.info("Ongoing update skipped for Auction {}, already set as '{}'", auction.hash, ongoing)
                    // Nothing to update, return null
                    null
                }
            }
        }
    }

    private suspend fun onAuctionUpdated(auction: Auction?, type: AuctionOffchainHistory.Type) {
        auction?.let {
            val history = toHistory(auction, type)
            auctionOffchainHistoryRepository.save(history)
            val event = auctionActivityConverter.convert(history, auction)
            eventPublisher.publish(event)

            logger.info("Ongoing state updated fo Auction {}: {} at {}", auction.hash, history.type, history.date)
        }
    }

    private fun toHistory(auction: Auction, type: AuctionOffchainHistory.Type): AuctionOffchainHistory {
        return AuctionOffchainHistory(
            hash = auction.hash,
            date = auction.startTime ?: auction.createdAt,
            contract = auction.contract,
            seller = auction.seller,
            sell = auction.sell,
            source = HistorySource.RARIBLE,
            type = type
        )
    }

}