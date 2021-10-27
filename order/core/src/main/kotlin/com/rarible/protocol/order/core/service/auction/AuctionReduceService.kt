package com.rarible.protocol.order.core.service.auction

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.util.Hash
import java.time.Duration
import java.time.Instant

@Component
class AuctionReduceService(
    private val auctionHistoryRepository: AuctionHistoryRepository,
    private val auctionRepository: AuctionRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(AuctionReduceService::class.java)

    suspend fun updateAuction(auctionHash: Word): Auction? = update(auctionHash = auctionHash).awaitFirstOrNull()

    fun update(auctionHash: Word?, fromAuctionHash: Word?): Flux<Auction> {
        logger.info("Update auction hash=$auctionHash fromHash=$fromAuctionHash")

        return auctionHistoryRepository.findLogEvents(auctionHash, fromAuctionHash)
            .map { logEvent -> AuctionUpdate(logEvent) }
            .windowUntilChanged { auctionUpdate -> auctionUpdate.auctionHash }
            .concatMap { updateAuction(it) }
    }

    private fun updateAuction(updates: Flux<AuctionUpdate>): Mono<Auction> = mono {
        val result = updates.asFlow().fold(EMPTY_ACTION) { auction, update ->
            when (update.logStatus) {
                LogEventStatus.CONFIRMED -> {

                }
                LogEventStatus.PENDING -> {

                }
            }
        }
        if (result.hash == EMPTY_ORDER_HASH) {
            logger.info("Order ${lastSeenUpdate?.orderHash} has not been reduced, none OrderVersion ware found")
            return@mono emptyOrder
        }
        updateOrderWithState(result)
    }

    private fun Order.updateWith(
        logEventStatus: LogEventStatus,
        orderExchangeHistory: OrderExchangeHistory,
        eventId: String
    ): Order {
        return when (logEventStatus) {
            LogEventStatus.PENDING -> copy(pending = pending + orderExchangeHistory)
            LogEventStatus.CONFIRMED -> when (orderExchangeHistory) {
                is OrderSideMatch -> copy(
                    fill = fill.plus(orderExchangeHistory.fill),
                    lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date),
                    lastEventId = accumulateEventId(lastEventId, eventId)
                )
                is OrderCancel -> copy(
                    cancelled = true,
                    lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date),
                    lastEventId = accumulateEventId(lastEventId, eventId)
                )
                is OnChainOrder -> error("OnChainOrder events must have been processed earlier.")
            }
            else -> this
        }
    }

    private suspend fun updateWith(
        accumulator: Order,
        version: OrderVersion,
        eventId: String
    ): Order {
        val previous = if (version.onChainOrderKey != null) {
            // On-chain orders can be re-opened, so we must start from the empty state again, even if there were previous events.
            emptyOrder
        } else {
            accumulator
        }

        return Order(
            maker = version.maker,
            taker = version.taker,
            make = version.make,
            take = version.take,
            type = version.type,
            salt = version.salt,
            start = version.start,
            end = version.end,
            data = version.data,
            signature = version.signature,
            makePriceUsd = version.makePriceUsd,
            takePriceUsd = version.takePriceUsd,
            makePrice = version.makePrice,
            takePrice = version.takePrice,
            makeUsd = version.makeUsd,
            takeUsd = version.takeUsd,
            platform = version.platform,
            hash = version.hash,

            createdAt = previous.createdAt.takeUnless { it == Instant.EPOCH } ?: version.createdAt,
            lastUpdateAt = version.createdAt,

            lastEventId = accumulateEventId(accumulator.lastEventId, eventId),

            priceHistory = getUpdatedPriceHistoryRecords(previous, version),
            fill = previous.fill,
            cancelled = previous.cancelled,
            makeStock = previous.makeStock,
            pending = previous.pending
        )
    }

    private suspend fun getUpdatedPriceHistoryRecords(
        previous: Order,
        orderVersion: OrderVersion
    ): List<OrderPriceHistoryRecord> {
        if (previous.make == orderVersion.make && previous.take == orderVersion.take) {
            return previous.priceHistory
        }
        val newRecord = OrderPriceHistoryRecord(
            orderVersion.createdAt,
            priceNormalizer.normalize(orderVersion.make),
            priceNormalizer.normalize(orderVersion.take)
        )
        return (listOf(newRecord) + previous.priceHistory).take(Order.MAX_PRICE_HISTORIES)
    }

    private suspend fun Order.withUpdatedMakeStock(): Order {
        val makeBalance = assetMakeBalanceProvider.getMakeBalance(this)
        logger.info("Make balance $makeBalance for order $hash")
        return withMakeBalance(makeBalance, protocolCommissionProvider.get())
    }

    private suspend fun Order.withNewPrice(): Order {
        val orderUsdValue = priceUpdateService.getAssetsUsdValue(make, take, nowMillis())
        return if (orderUsdValue != null) withOrderUsdValue(orderUsdValue) else this
    }

    private suspend fun updateOrderWithState(orderStub: Order): Order {
        val order = orderStub
            .withUpdatedMakeStock()
            .withNewPrice()
        val saved = orderRepository.save(order)
        logger.info(buildString {
            append("Updated order: ")
            append("hash=${saved.hash}, ")
            append("makeStock=${saved.makeStock}, ")
            append("fill=${saved.fill}, ")
            append("cancelled=${saved.cancelled}, ")
            append("signature=${saved.signature}, ")
            append("pendingSize=${saved.pending.size},")
            append("status=${saved.status}")
        })
        return saved
    }

    companion object {
        private val EMPTY_AUCTION_HASH = 0.toBigInteger().toWord()

        private val EMPTY_ACTION = Auction(
            type = AuctionType.RARIBLE_V1,
            seller = Address.ZERO(),
            buyer = null,
            sell = Asset(EthAssetType, EthUInt256.ZERO),
            buy = EthAssetType,
            lastBid = null,
            startTime = Instant.EPOCH,
            endTime = Instant.EPOCH,
            minimalStep = EthUInt256.ZERO,
            minimalPrice = EthUInt256.ZERO,
            canceled = false,
            data = RaribleAuctionV1DataV1(
                originFees = emptyList(),
                duration = Duration.ZERO,
                startTime = Instant.EPOCH,
                buyOutPrice = EthUInt256.ZERO
            ),
            createdAt = Instant.EPOCH,
            lastUpdatedAy = Instant.EPOCH,
            auctionId = EthUInt256.ZERO,
            protocolFee = EthUInt256.ZERO,
            contract = Address.ZERO()
        )

        private fun accumulateEventId(lastEventId: String?, eventId: String): String {
            return Hash.sha3((lastEventId ?: "") + eventId)
        }

        private val wordComparator = Comparator<Word> r@{ w1, w2 ->
            val w1Bytes = w1.bytes()
            val w2Bytes = w2.bytes()
            for (i in 0 until minOf(w1Bytes.size, w2Bytes.size)) {
                if (w1Bytes[i] != w2Bytes[i]) {
                    return@r w1Bytes[i].compareTo(w2Bytes[i])
                }
            }
            return@r w1Bytes.size.compareTo(w2Bytes.size)
        }

        private fun OrderUpdate.toLogEventKey() = when (this) {
            is OrderUpdate.ByLogEvent -> logEvent.toLogEventKey()
            is OrderUpdate.ByOrderVersion -> orderVersion.onChainOrderKey
        }

        private val orderUpdateComparator: Comparator<OrderUpdate> = Comparator r@{ u1, u2 ->
            wordComparator.compare(u1.orderHash, u2.orderHash).takeUnless { it == 0 }?.let { return@r it }
            val k1 = u1.toLogEventKey()
            val k2 = u2.toLogEventKey()
            if (k1 == null || k2 == null) {
                return@r u1.date.compareTo(u2.date)
            }
            return@r k1.compareTo(k2)
        }

        private class AuctionUpdate(private val logEvent: LogEvent) {
            val logStatus get() = logEvent.status
            val history get() = logEvent.data.toAuctionHistory()
            val auctionHash get() = history.hash
            val eventId: String get() = logEvent.id.toHexString()
            val date get() = logEvent.updatedAt

            private fun EventData.toAuctionHistory(): AuctionHistory {
                return requireNotNull(this as? AuctionHistory) { "Unexpected auction history type ${this::class}" }
            }
        }
    }
}
