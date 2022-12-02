package com.rarible.protocol.order.core.service.auction

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.reduce.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionCancelled
import com.rarible.protocol.order.core.model.AuctionFinished
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.AuctionReduceEvent
import com.rarible.protocol.order.core.model.AuctionReduceSnapshot
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.core.model.AuctionType
import com.rarible.protocol.order.core.model.BaseAuction
import com.rarible.protocol.order.core.model.BidPlaced
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OnChainAuction
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.RaribleAuctionV1DataV1
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.util.Hash
import java.time.Instant

@Component
class AuctionReducer : Reducer<AuctionReduceEvent, AuctionReduceSnapshot, Long, Auction, Word> {

    override suspend fun reduce(initial: AuctionReduceSnapshot, event: AuctionReduceEvent): AuctionReduceSnapshot {
        val hash = initial.id
        val auction = initial.data
        val update = AuctionUpdate(event.logEvent)

        val result = when (update.logStatus) {
            EthereumLogStatus.CONFIRMED -> {
                auction.withUpdate(update)
            }
            EthereumLogStatus.PENDING -> {
                auction.withUpdate(update).withPending(update)
            }
            else -> auction
        }
        return AuctionReduceSnapshot(hash, result.withCalculatedState(), event.mark)
    }

    override fun getDataKeyFromEvent(event: AuctionReduceEvent): Word {
        return event.logEvent.data.toAuctionHistory().hash
    }

    override fun getInitialData(key: Word): AuctionReduceSnapshot {
        return AuctionReduceSnapshot(key, EMPTY_ACTION, Long.MIN_VALUE)
    }

    private fun Auction.withUpdate(auctionUpdate: AuctionUpdate): Auction {
        val lastEventId = accumulateEventId(lastEventId, auctionUpdate.eventId)

        return when (val history = auctionUpdate.history) {
            is OnChainAuction -> withBaseAuction(history).copy(
                type = history.auctionType,
                finished = false,
                cancelled = false,
                lastUpdateAt = history.date,
                startTime = history.data.startTime,
                createdAt = history.date,
                auctionId = history.auctionId,
                contract = auctionUpdate.contract,
                lastEventId = lastEventId
            )
            is BidPlaced -> copy(
                buyer = history.buyer,
                lastBid = history.bid,
                lastUpdateAt = history.date,
                lastEventId = lastEventId
            )
            is AuctionCancelled -> copy(
                cancelled = true,
                lastUpdateAt = history.date
            )
            is AuctionFinished -> withBaseAuction(history).copy(
                finished = true,
                lastUpdateAt = history.date,
                lastEventId = lastEventId
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

    private fun accumulateEventId(lastEventId: String?, eventId: String): String {
        return Hash.sha3((lastEventId ?: "") + eventId)
    }

    companion object {
        private val EMPTY_ACTION = Auction(
            type = AuctionType.RARIBLE_V1,
            status = AuctionStatus.ACTIVE,
            seller = Address.ZERO(),
            buyer = null,
            sell = Asset(EthAssetType, EthUInt256.ZERO),
            buy = EthAssetType,
            lastBid = null,
            startTime = null,
            endTime = Instant.EPOCH,
            minimalStep = EthUInt256.ZERO,
            minimalPrice = EthUInt256.ZERO,
            ongoing = false,
            finished = false,
            cancelled = false,
            deleted = false,
            data = RaribleAuctionV1DataV1(
                originFees = emptyList(),
                payouts = emptyList(),
                duration = EthUInt256.ZERO,
                startTime = null,
                buyOutPrice = EthUInt256.ZERO
            ),
            createdAt = Instant.EPOCH,
            lastUpdateAt = Instant.EPOCH,
            lastEventId = null,
            auctionId = EthUInt256.ZERO,
            protocolFee = EthUInt256.ZERO,
            platform = Platform.RARIBLE,
            contract = Address.ZERO(),
            buyPrice = null,
            buyPriceUsd = null,
            pending = emptyList()
        )

        private class AuctionUpdate(private val logEvent: ReversedEthereumLogRecord) {
            val contract get() = logEvent.address
            val logStatus get() = logEvent.status
            val history get() = logEvent.data.toAuctionHistory()
            val eventId: String get() = logEvent.id
        }

        private fun EventData.toAuctionHistory(): AuctionHistory {
            return requireNotNull(this as? AuctionHistory) { "Unexpected auction history type ${this::class}" }
        }
    }
}

