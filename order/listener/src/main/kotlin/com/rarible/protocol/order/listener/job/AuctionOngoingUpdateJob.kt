package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.core.service.auction.AuctionStateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
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
    private val auctionStateService: AuctionStateService
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.updateAuctionOngoingState}")
    @CaptureTransaction(value = "auction_ongoing_update")
    fun execute() = runBlocking<Unit> {
        if (properties.updateAuctionOngoingStateEnabled.not()) return@runBlocking

        auctionRepository.findOngoingNotUpdatedIds().collect {
            val auction = auctionStateService.updateOngoingState(it, true)
            auctionStateService.onAuctionOngoingStateUpdated(auction, AuctionOffchainHistory.Type.STARTED)
        }
        auctionRepository.findEndedNotUpdatedIds(properties.updateAuctionOngoingStateEndLag).collect {
            val auction = auctionStateService.updateOngoingState(it, false)
            auctionStateService.onAuctionOngoingStateUpdated(auction, AuctionOffchainHistory.Type.ENDED)
        }
    }


}