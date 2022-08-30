package com.rarible.protocol.order.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.converters.model.PlatformToHistorySourceConverter
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.daonomic.rpc.domain.Word
import java.time.Instant
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.util.Hash

@Component
@CaptureSpan(type = SpanType.APP)
class OrderReduceService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val assetMakeBalanceProvider: AssetMakeBalanceProvider,
    private val protocolCommissionProvider: ProtocolCommissionProvider,
    private val priceNormalizer: PriceNormalizer,
    private val priceUpdateService: PriceUpdateService,
    private val nonceService: NonceService,
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val raribleOrderExpiration: OrderIndexerProperties.RaribleOrderExpirationProperties,
    private val approvalHistoryRepository: ApprovalHistoryRepository
) {
    suspend fun updateOrder(orderHash: Word): Order? = update(orderHash = orderHash).awaitFirstOrNull()

    // TODO: current reduce implementation does not guarantee we will save the latest Order, see RPN-921.
    fun update(orderHash: Word? = null, fromOrderHash: Word? = null, platforms: List<Platform>? = null): Flux<Order> {
        logger.info("Update hash=$orderHash fromHash=$fromOrderHash")
        @Suppress("DEPRECATION")
        return Flux.mergeOrdered(
            orderUpdateComparator,
            orderVersionRepository.findAllByHash(orderHash, fromOrderHash, platforms)
                .map { OrderUpdate.ByOrderVersion(it) },
            exchangeHistoryRepository.findLogEvents(orderHash, fromOrderHash, platforms?.map(PlatformToHistorySourceConverter::convert))
                .map { OrderUpdate.ByLogEvent(it) }
        )
            .windowUntilChanged { it.orderHash }
            .concatMap {
                it.switchOnFirst { first, logs ->
                    val log = first.get()
                    if (log != null) {
                        updateOrder(log.orderHash, logs)
                    } else {
                        Mono.empty()
                    }
                }
            }
    }

    private sealed class OrderUpdate {
        abstract val orderHash: Word
        abstract val eventId: ObjectId

        data class ByOrderVersion(val orderVersion: OrderVersion) : OrderUpdate() {
            override val orderHash get() = orderVersion.hash
            override val eventId get() = orderVersion.id
        }

        data class ByLogEvent(val logEvent: LogEvent) : OrderUpdate() {
            override val orderHash get() = logEvent.data.toExchangeHistory().hash
            override val eventId get() = logEvent.id
        }
    }

    private fun updateOrder(hash: Word, updates: Flux<OrderUpdate>): Mono<Order> = mono {
        val version = orderRepository.findById(hash)?.version
        // Fields used for logging only.
        var seenRevertedOnChainOrder = false
        var seenOrderHash: Word? = null

        val result = updates.asFlow().fold(emptyOrder) { order, update ->
            seenOrderHash = update?.orderHash
            when (update) {
                is OrderUpdate.ByOrderVersion -> {
                    if (update.orderVersion.onChainOrderKey != null) {
                        // On-chain order versions are processed via the OnChainOrder LogEvent-s in the next when-branch.
                        order
                    } else {
                        order.updateWith(update.orderVersion, update.eventId.toHexString())
                    }
                }
                is OrderUpdate.ByLogEvent -> {
                    val exchangeHistory = update.logEvent.data.toExchangeHistory()
                    if (exchangeHistory is OnChainOrder && update.logEvent.status != LogEventStatus.CONFIRMED) {
                        seenRevertedOnChainOrder = true
                    }
                    order.updateWith(update.logEvent, exchangeHistory, update.eventId.toHexString())
                }
            }
        }
        /*
        Resulting order may have EMPTY_ORDER_HASH in two cases:
        1) There were neither OrderVersion-s (for API versions) nor OnChainOrder-s (for on-chain orders) for this hash.
           => We don't have enough data to construct an order.
        2) There were some OnChainOrder-s, but they were reverted.
           => We have to remove the order from the database.
         */
        if (result.hash == EMPTY_ORDER_HASH) {
            logger.info(buildString {
                append("Order $seenOrderHash reduce ended up with empty order: ")
                append(
                    if (seenRevertedOnChainOrder) {
                        "the on-chain order was reverted"
                    } else {
                        "there were no OrderVersion-s for this hash"
                    }
                )
            })
            if (seenOrderHash != null) {
                // Remove the possibly reverted order from the OrderRepository.
                orderRepository.remove(seenOrderHash!!)
            }
            return@mono emptyOrder
        }
        updateOrderWithState(result.withVersion(version))
    }

    private suspend fun Order.updateWith(
        logEvent: LogEvent,
        orderExchangeHistory: OrderExchangeHistory,
        eventId: String
    ): Order {
        if (orderExchangeHistory is OnChainOrder) {
            return updateWithOnChainOrder(logEvent, orderExchangeHistory, eventId)
        }
        @Suppress("KotlinConstantConditions")
        return when (logEvent.status) {
            LogEventStatus.CONFIRMED -> when (orderExchangeHistory) {
                is OrderSideMatch -> {
                    if (orderExchangeHistory.adhoc == true && type == OrderType.CRYPTO_PUNKS) {
                        /*
                         * Do not apply side matches to "virtual" orders, which are created solely for the exchange moment.
                         * This only happens to on-chain orders that have the same salt for all events.
                         * For now, this is only about CryptoPunks: their orders always have salt = 0.
                         * Consider the case:
                         * 1) Owner puts a punk on sale
                         * 2) Bidder makes a bid on the punk
                         * 3) Owner accepts the bid
                         * In this case the sell order (p. 1) must be cancelled, not filled.
                         * This is why when we're processing the OrderSideMatch for the virtual "sell" order of a bid match
                         * we must not apply it to the real sell order out there in the database.
                         */
                        this
                    } else {
                        copy(
                            fill = fill.plus(orderExchangeHistory.fill),
                            lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date),
                            lastEventId = accumulateEventId(lastEventId, eventId)
                        )
                    }
                }
                is OrderCancel -> copy(
                    cancelled = true,
                    lastUpdateAt = maxOf(lastUpdateAt, orderExchangeHistory.date),
                    lastEventId = accumulateEventId(lastEventId, eventId)
                )
                is OnChainOrder -> error("Must have been processed above")
                is OnChainAmmOrder -> TODO()
            }
            else -> this
        }
    }

    private suspend fun Order.updateWithOnChainOrder(
        logEvent: LogEvent,
        onChainOrder: OnChainOrder,
        eventId: String
    ): Order {
        val onChainOrderKey = logEvent.toLogEventKey()
        return if (logEvent.status == LogEventStatus.CONFIRMED) {
            val orderVersion = onChainOrder.toOrderVersion()
                .copy(onChainOrderKey = onChainOrderKey)
                .let { priceUpdateService.withUpdatedAllPrices(it) }
            if (!orderVersionRepository.existsByOnChainOrderKey(onChainOrderKey).awaitFirst()) {
                try {
                    orderVersionRepository.save(orderVersion).awaitFirst()
                } catch (ignored: DuplicateKeyException) {
                }
            }
            // On-chain orders can be re-opened, so we start from the empty state.
            emptyOrder.updateWith(orderVersion, eventId)
        } else {
            orderVersionRepository.deleteByOnChainOrderKey(onChainOrderKey).awaitFirstOrNull()
            // Skip this reverted log.
            this
        }
    }

    private suspend fun Order.updateWith(
        version: OrderVersion,
        eventId: String
    ): Order = Order(
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

        createdAt = createdAt.takeUnless { it == Instant.EPOCH } ?: version.createdAt,
        lastUpdateAt = version.createdAt,

        lastEventId = accumulateEventId(lastEventId, eventId),

        priceHistory = getUpdatedPriceHistoryRecords(this, version),
        fill = fill,
        cancelled = cancelled,
        makeStock = makeStock
    )

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
        val lastUpdatedAt = makeBalance.lastUpdatedAt
        val copy = if (lastUpdatedAt != null && this.lastUpdateAt.isBefore(lastUpdatedAt)) {
            this.copy(lastUpdateAt = lastUpdatedAt)
        } else {
            this
        }
        return copy.withMakeBalance(makeBalance.value, protocolCommissionProvider.get())

    }

    private suspend fun Order.withNewPrice(): Order {
        val orderUsdValue = priceUpdateService.getAssetsUsdValue(make, take, nowMillis())
        return if (orderUsdValue != null) withOrderUsdValue(orderUsdValue) else this
    }

    private suspend fun Order.withUpdatedCounter(): Order {
        val data = this.data as? OrderCounterableData ?: return this
        val makerCounter = nonceService.getLatestMakerNonce(this.maker, this.protocol)
        return if (data.isValidCounter(makerCounter.nonce.value.toLong()).not()) {
            logger.info("Cancel order $hash as order counter is not match current maker counter ${makerCounter.nonce}")
            this.copy(
                cancelled = true,
                lastUpdateAt = maxOf(this.lastUpdateAt, makerCounter.timestamp),
                lastEventId = accumulateEventId(this.lastEventId, makerCounter.historyId)
            )
        } else {
            this
        }
    }

    private suspend fun Order.withCancelOpenSea(): Order {
        if (this.type != OrderType.OPEN_SEA_V1) return this
        val exchange = (this.data as? OrderOpenSeaV1DataV1)?.exchange ?: return this
        val lastUpdateAt = if (exchangeContractAddresses.openSeaV1 == exchange) 1645812000L else 1659366000L
        val affectedStatuses = arrayOf(OrderStatus.NOT_STARTED, OrderStatus.INACTIVE, OrderStatus.ACTIVE)
        return if (this.status in affectedStatuses) {
            logger.info("Cancel order $hash as OpenSea exchangeV1/V2 contract was expired")
            this.copy(
                cancelled = true,
                lastUpdateAt = maxOf(this.lastUpdateAt, Instant.ofEpochSecond(lastUpdateAt)),
                lastEventId = accumulateEventId(this.lastEventId, exchange.toString())
            )
        } else {
            this
        }
    }

    private suspend fun Order.withBidExpire(): Order {
        val expiredDate = Instant.now() - raribleOrderExpiration.bidExpirePeriod

        if (this.isBid().not()) return this
        if (this.platform != Platform.RARIBLE) return this
        if (this.status !in listOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE)) return this
        if (this.lastUpdateAt > expiredDate) return this

        logger.info("Cancel rarible BID $hash cause it expired after $expiredDate")
        return this.copy(
            status = OrderStatus.CANCELLED,
            lastUpdateAt = Instant.now(),
            dbUpdatedAt = Instant.now(),
            cancelled = true,
            lastEventId = accumulateEventId(this.lastEventId, "$expiredDate")
        )
    }

    private suspend fun Order.withApproval(): Order {
        if (this.status != OrderStatus.ACTIVE) return this
        return if (this.isBid()) withBidApproval() else withSellApproval()
    }

    private suspend fun Order.withSellApproval(): Order {
        val collection = when(val assetType = this.make.type) {
            is Erc721AssetType -> assetType.token
            is CollectionAssetType -> assetType.token
            is GenerativeArtAssetType -> assetType.token
            is CryptoPunksAssetType -> assetType.token
            is Erc1155AssetType -> assetType.token
            is Erc1155LazyAssetType -> assetType.token
            is Erc721LazyAssetType -> assetType.token
            else -> return this
        }
        val lastApprovalEvent = approvalHistoryRepository.lastApprovalLogEvent(collection, this.maker) ?: return this
        val approveInfo = lastApprovalEvent.data as ApprovalHistory
        return if(approveInfo.approved != this.approved) {
            logger.info("Change order $hash approval to ${approveInfo.approved}!")
            this.copy(
                approved = approveInfo.approved,
                lastEventId = accumulateEventId(this.lastEventId, lastApprovalEvent.id.toHexString())
            )
        } else this
    }

    private suspend fun Order.withBidApproval(): Order = this //todo support ERC20 Approve

    private suspend fun updateOrderWithState(orderStub: Order): Order {
        val order = orderStub
            .withUpdatedMakeStock()
            .withNewPrice()
            .withUpdatedCounter()
            .withCancelOpenSea()
            .withApproval()
            .withBidExpire()

        val saved = orderRepository.save(order)
        logger.info(buildString {
            append("Updated order: ")
            append("hash=${saved.hash}, ")
            append("makeStock=${saved.makeStock}, ")
            append("fill=${saved.fill}, ")
            append("cancelled=${saved.cancelled}, ")
            append("signature=${saved.signature}, ")
            append("status=${saved.status}")
        })
        return saved
    }

    private val Order.protocol: Address
        get() = when (type) {
            OrderType.RARIBLE_V1 -> exchangeContractAddresses.v1
            OrderType.OPEN_SEA_V1 -> (data as OrderOpenSeaV1DataV1).exchange
            OrderType.SEAPORT_V1 -> (data as OrderSeaportDataV1).protocol
            OrderType.CRYPTO_PUNKS -> exchangeContractAddresses.cryptoPunks
            OrderType.RARIBLE_V2 -> exchangeContractAddresses.v2
            OrderType.LOOKSRARE -> exchangeContractAddresses.looksrareV1
            OrderType.X2Y2 -> exchangeContractAddresses.x2y2V1
            OrderType.AMM -> (data as OrderAmmData).contract
        }

    companion object {
        val EMPTY_ORDER_HASH = 0.toBigInteger().toWord()

        private val emptyOrder = Order(
            maker = Address.ZERO(),
            taker = Address.ZERO(),
            make = Asset(EthAssetType, EthUInt256.ZERO),
            take = Asset(EthAssetType, EthUInt256.ZERO),
            type = OrderType.RARIBLE_V2,
            fill = EthUInt256.ZERO,
            cancelled = false,
            makeStock = EthUInt256.ZERO,
            salt = EthUInt256.ZERO,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = Instant.EPOCH,
            lastUpdateAt = Instant.EPOCH,
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            priceHistory = emptyList(),
            platform = Platform.RARIBLE,
            hash = EMPTY_ORDER_HASH
        )

        private fun accumulateEventId(lastEventId: String?, eventId: String): String {
            return Hash.sha3((lastEventId ?: "") + eventId)
        }

        /**
         * This comparator MUST comply with the order used in Fluxes for mergeOrdered.
         * Also, it explicitly moves OrderUpdate.ByOrderVersion before OrderUpdate.ByLogEvent
         */
        private val orderUpdateComparator: Comparator<OrderUpdate> = Comparator r@{ u1, u2 ->
            u1.orderHash.toString().compareTo(u2.orderHash.toString()).takeUnless { it == 0 }?.let { return@r it }
            if (u1 is OrderUpdate.ByOrderVersion && u2 is OrderUpdate.ByOrderVersion) {
                return@r u1.eventId.compareTo(u2.eventId)
            }
            if (u1 is OrderUpdate.ByLogEvent && u2 is OrderUpdate.ByLogEvent) {
                return@r logEventComparator.compare(u1.logEvent, u2.logEvent)
            }
            if (u1 is OrderUpdate.ByOrderVersion && u2 is OrderUpdate.ByLogEvent) return@r -1
            return@r 1
        }

        private val logEventComparator = compareBy<LogEvent> { it.blockNumber ?: 0 } then
            compareBy { it.logIndex ?: 0 } then
            compareBy { it.minorLogIndex }

        val logger: Logger = LoggerFactory.getLogger(OrderReduceService::class.java)

        fun EventData.toExchangeHistory(): OrderExchangeHistory =
            requireNotNull(this as? OrderExchangeHistory) { "Unexpected exchange history type ${this::class}" }
    }
}
