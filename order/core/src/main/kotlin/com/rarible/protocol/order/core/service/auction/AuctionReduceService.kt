package com.rarible.protocol.order.core.service.auction

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.time.Instant

@Component
class AuctionReduceService(
    private val auctionHistoryRepository: AuctionHistoryRepository,
    private val auctionRepository: AuctionRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateAuction(auctionHash: Word): Auction? = update(auctionHash = auctionHash).awaitFirstOrNull()

    fun update(auctionHash: Word? = null, fromAuctionHash: Word? = null): Flux<Auction> {
        logger.info("Update auction hash=$auctionHash fromHash=$fromAuctionHash")

        return auctionHistoryRepository.findLogEvents(auctionHash, fromAuctionHash)
            .map { logEvent -> AuctionUpdate(logEvent) }
            .windowUntilChanged { auctionUpdate -> auctionUpdate.auctionHash }
            .concatMap { updateAuction(it) }
    }

    private fun updateAuction(updates: Flux<AuctionUpdate>): Mono<Auction> = mono {
        var lastSeenUpdate: AuctionUpdate? = null

        val result = updates.asFlow().fold(EMPTY_ACTION) { auction, update ->
            lastSeenUpdate = update

            when (update.logStatus) {
                LogEventStatus.CONFIRMED -> {
                    auction.withUpdate(update)
                }
                LogEventStatus.PENDING -> {
                    auction.withUpdate(update).withPending(update)
                }
                else -> auction
            }
        }
        if (result.hash == EMPTY_AUCTION_HASH) {
            logger.info("Auction ${lastSeenUpdate?.auctionHash} has not been reduced, none OnChainAuction event ware found")
            return@mono EMPTY_ACTION
        }
        updateAuctionWithState(result)
    }

    private fun Auction.withUpdate(auctionUpdate: AuctionUpdate): Auction {
        return when (val history = auctionUpdate.history) {
            is OnChainAuction -> withBaseAuction(history).copy(
                type = history.auctionType,
                finished = false,
                cancelled = false,
                lastUpdatedAy = history.date,
                createdAt = history.date,
                auctionId = history.auctionId,
                contract = auctionUpdate.contract
            )
            is BidPlaced -> copy(
                buyer = history.buyer,
                lastBid = history.bid,
                lastUpdatedAy = history.date
            )
            is AuctionCancelled -> copy(
                cancelled = true,
                lastUpdatedAy = history.date
            )
            is AuctionFinished -> withBaseAuction(history).copy(
                finished = true,
                lastUpdatedAy = history.date
            )
        }
    }

    private fun Auction.withPending(auctionUpdate: AuctionUpdate): Auction {
        return copy(pending = pending + auctionUpdate.history)
    }

    private fun Auction.withBaseAuction(baseAuction: BaseAuction): Auction {
        return copy(
            seller = baseAuction.seller,
            buyer = baseAuction.buyer,
            sell =  baseAuction.sell,
            buy = baseAuction.buy,
            lastBid = baseAuction.lastBid,
            endTime = baseAuction.endTime,
            minimalStep = baseAuction.minimalStep,
            minimalPrice = baseAuction.minimalPrice,
            data = baseAuction.data,
            protocolFee = baseAuction.protocolFee
        )
    }

    private suspend fun updateAuctionWithState(auction: Auction): Auction {
        val saved = auctionRepository.save(auction.withCalculatedSate())
        logger.info(buildString {
            append("Updated auction: ")
            append("hash=${saved.hash}, ")
            append("finished=${saved.finished}, ")
            append("cancelled=${saved.cancelled}, ")
            append("status=${saved.status}, ")
            append("pendingSize=${saved.pending.size},")
        })
        return saved
    }

    companion object {
        private val EMPTY_AUCTION_HASH = 0.toBigInteger().toWord()

        private val EMPTY_ACTION = Auction(
            type = AuctionType.RARIBLE_V1,
            status = AuctionStatus.ACTIVE,
            seller = Address.ZERO(),
            buyer = null,
            sell = Asset(EthAssetType, EthUInt256.ZERO),
            buy = EthAssetType,
            lastBid = null,
            endTime = Instant.EPOCH,
            minimalStep = EthUInt256.ZERO,
            minimalPrice = EthUInt256.ZERO,
            finished = false,
            cancelled = false,
            data = RaribleAuctionV1DataV1(
                originFees = emptyList(),
                payouts = emptyList(),
                duration = EthUInt256.ZERO,
                startTime = EthUInt256.ZERO,
                buyOutPrice = EthUInt256.ZERO
            ),
            createdAt = Instant.EPOCH,
            lastUpdatedAy = Instant.EPOCH,
            auctionId = EthUInt256.ZERO,
            protocolFee = EthUInt256.ZERO,
            contract = Address.ZERO(),
            pending = emptyList()
        )

        private class AuctionUpdate(private val logEvent: LogEvent) {
            val contract get() = logEvent.address
            val logStatus get() = logEvent.status
            val history get() = logEvent.data.toAuctionHistory()
            val auctionHash get() = history.hash

            private fun EventData.toAuctionHistory(): AuctionHistory {
                return requireNotNull(this as? AuctionHistory) { "Unexpected auction history type ${this::class}" }
            }
        }
    }
}
