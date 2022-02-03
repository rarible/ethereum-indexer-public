package com.rarible.protocol.order.core.service.auction

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.producer.ProtocolAuctionPublisher
import com.rarible.protocol.order.core.repository.auction.AuctionOffchainHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AuctionStateService(
    private val auctionRepository: AuctionRepository,
    private val auctionOffchainHistoryRepository: AuctionOffchainHistoryRepository,
    private val eventPublisher: ProtocolAuctionPublisher,
    private val auctionActivityConverter: AuctionActivityConverter
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateOngoingState(hash: Word, ongoing: Boolean): Auction? {
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

    suspend fun onAuctionOngoingStateUpdated(auction: Auction?, type: AuctionOffchainHistory.Type) {
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
