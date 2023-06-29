package com.rarible.protocol.order.core.service

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.converters.model.PlatformToHistorySourceConverter
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import com.rarible.protocol.order.core.service.pool.EventPoolReducer
import com.rarible.protocol.order.core.service.pool.PoolPriceProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.time.Instant

@Component
@CaptureSpan(type = SpanType.APP)
class OrderReduceService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val poolHistoryRepository: PoolHistoryRepository,
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val assetMakeBalanceProvider: AssetMakeBalanceProvider,
    private val protocolCommissionProvider: ProtocolCommissionProvider,
    private val priceNormalizer: PriceNormalizer,
    private val priceUpdateService: PriceUpdateService,
    private val nonceService: NonceService,
    private val indexerProperties: OrderIndexerProperties,
    private val approveService: ApproveService,
    private val poolReducer: EventPoolReducer,
    private val poolPriceProvider: PoolPriceProvider,
    private val orderStateRepository: OrderStateRepository,
) {

    private val exchangeContractAddresses = indexerProperties.exchangeContractAddresses
    private val raribleOrderExpiration = indexerProperties.raribleOrderExpiration

    suspend fun updateOrder(orderHash: Word): Order? = update(orderHash = orderHash).awaitFirstOrNull()

    fun update(orderHash: Word? = null, fromOrderHash: Word? = null, platforms: List<Platform>? = null): Flux<Order> {
        logger.info("Update hash=$orderHash fromHash=$fromOrderHash")
        @Suppress("DEPRECATION")
        return Flux.mergeOrdered(
            orderUpdateComparator,
            orderVersionRepository.findAllByHash(orderHash, fromOrderHash, platforms)
                .map { OrderUpdate.ByOrderVersion(it) },
            exchangeHistoryRepository.findReversedEthereumLogRecords(
                orderHash,
                fromOrderHash,
                platforms?.map(PlatformToHistorySourceConverter::convert)
            )
                .map { OrderUpdate.ByExchangeLogEvent(it) },
            poolHistoryRepository.findReversedEthereumLogRecords(
                orderHash,
                fromOrderHash,
                platforms?.map(PlatformToHistorySourceConverter::convert)
            )
                .map { OrderUpdate.ByPoolLogEvent(it) },
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
        abstract val eventId: String

        data class ByOrderVersion(val orderVersion: OrderVersion) : OrderUpdate() {

            override val orderHash get() = orderVersion.hash
            override val eventId get() = orderVersion.id.toHexString()
        }

        data class ByExchangeLogEvent(val logEvent: ReversedEthereumLogRecord) : OrderUpdate() {

            override val orderHash get() = logEvent.data.toExchangeHistory().hash
            override val eventId get() = logEvent.id
        }

        data class ByPoolLogEvent(val logEvent: ReversedEthereumLogRecord) : OrderUpdate() {

            override val orderHash get() = logEvent.data.toPoolHistory().hash
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
                        order.updateWith(update.orderVersion, update.eventId)
                    }
                }

                is OrderUpdate.ByExchangeLogEvent -> {
                    val exchangeHistory = update.logEvent.data.toExchangeHistory()

                    if (exchangeHistory.isOnChainOrder() && update.logEvent.status != EthereumBlockStatus.CONFIRMED) {
                        seenRevertedOnChainOrder = true
                    }
                    order.updateWith(update.logEvent, exchangeHistory, update.eventId)
                }

                is OrderUpdate.ByPoolLogEvent -> {
                    val poolHistory = update.logEvent.data.toPoolHistory()

                    if (poolHistory.isOnChainAmmOrder() && update.logEvent.status != EthereumBlockStatus.CONFIRMED) {
                        seenRevertedOnChainOrder = true
                    }
                    order.updateWith(update.logEvent, poolHistory, update.eventId)
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
        logEvent: ReversedEthereumLogRecord,
        orderExchangeHistory: OrderExchangeHistory,
        eventId: String
    ): Order {
        if (orderExchangeHistory is OnChainOrder) {
            return updateWithOnChainOrder(logEvent, orderExchangeHistory, eventId)
        }
        @Suppress("KotlinConstantConditions")
        return when (logEvent.status) {
            EthereumBlockStatus.CONFIRMED -> when (orderExchangeHistory) {
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
            }

            else -> this
        }
    }

    private suspend fun Order.updateWith(
        logEvent: ReversedEthereumLogRecord,
        poolHistory: PoolHistory,
        eventId: String
    ): Order {
        return when (logEvent.status) {
            EthereumBlockStatus.CONFIRMED -> {
                poolReducer.reduce(this, poolHistory).copy(
                    lastEventId = accumulateEventId(lastEventId, eventId),
                    lastUpdateAt = maxOf(lastUpdateAt, poolHistory.date),
                )
            }

            else -> this
        }
    }

    private suspend fun Order.updateWithOnChainOrder(
        logEvent: ReversedEthereumLogRecord,
        onChainOrder: OnChainOrder,
        eventId: String
    ): Order {
        val onChainOrderKey = logEvent.toLogEventKey()
        return if (logEvent.status == EthereumBlockStatus.CONFIRMED) {
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
        id = Order.Id(version.hash),
        hash = version.hash,
        approved = version.approved,

        createdAt = createdAt.takeUnless { it == Instant.EPOCH } ?: version.createdAt,
        lastUpdateAt = version.createdAt,

        lastEventId = accumulateEventId(lastEventId, eventId),

        priceHistory = getUpdatedPriceHistoryRecords(this, version),
        fill = fill,
        cancelled = cancelled,
        makeStock = makeStock,
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
        logger.info("Make balance $makeBalance for order $id")
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

    private suspend fun Order.withUpdatedPoolPrice(): Order {
        if (this.type != OrderType.AMM) return this
        return poolPriceProvider.updatePoolPrice(this)
    }

    private suspend fun Order.withUpdatedCounter(): Order {
        val data = this.data as? OrderCountableData ?: return this
        val makerCounter = nonceService.getLatestMakerNonce(this.maker, this.protocol)
        return if (data.isValidCounter(makerCounter.nonce.value).not()) {
            logger.info("Cancel order $id as order counter is not match current maker counter ${makerCounter.nonce}")
            this.copy(
                cancelled = true,
                lastUpdateAt = maxOf(this.lastUpdateAt, makerCounter.timestamp),
                lastEventId = accumulateEventId(this.lastEventId, makerCounter.historyId)
            )
        } else {
            this
        }
    }

    // TODO all this functions should be refactored as separate contributors
    private val openseaAffectedStatuses = setOf(OrderStatus.NOT_STARTED, OrderStatus.INACTIVE, OrderStatus.ACTIVE)

    private suspend fun Order.withCancelOpenSea(): Order {
        if (this.type != OrderType.OPEN_SEA_V1) return this
        val exchange = (this.data as? OrderOpenSeaV1DataV1)?.exchange ?: return this
        val lastUpdateAt = if (exchangeContractAddresses.openSeaV1 == exchange) 1645812000L else 1659366000L
        return if (this.status in openseaAffectedStatuses) {
            logger.info("Cancel order $id as OpenSea exchangeV1/V2 contract was expired")
            this.copy(
                cancelled = true,
                lastUpdateAt = maxOf(this.lastUpdateAt, Instant.ofEpochSecond(lastUpdateAt)),
                lastEventId = accumulateEventId(this.lastEventId, exchange.toString())
            )
        } else {
            this
        }
    }

    private suspend fun Order.withCancelSeaport(): Order {
        if (this.type != OrderType.SEAPORT_V1) return this
        val protocol = (this.data as? OrderBasicSeaportDataV1)?.protocol ?: return this
        if (!openseaAffectedStatuses.contains(this.status)) {
            return this
        }

        val disabledAt = when (protocol) {
            // The day when seaport v1.1 has been disabled (05.05.2023)
            exchangeContractAddresses.seaportV1 -> 1680652800L
            // The day when seaport v1.1 has been disabled (05.16.2023)
            exchangeContractAddresses.seaportV1_4 -> 1684195200L
            else -> return this
        }

        logger.info("Cancel order $id - SeaPort v1.1/1.4 is not supported anymore")
        return this.copy(
            cancelled = true,
            lastUpdateAt = maxOf(this.lastUpdateAt, Instant.ofEpochSecond(disabledAt)),
            lastEventId = accumulateEventId(this.lastEventId, protocol.toString())
        )
    }

    private suspend fun Order.withCancelSmallPriceSeaport(): Order {
        if (this.type != OrderType.SEAPORT_V1) return this

        if (this.makePrice != null && this.makePrice <= indexerProperties.minSeaportMakePrice) {
            logger.info("Cancel order $id as Seaport with small price")
            return this.copy(
                cancelled = true,
                status = OrderStatus.CANCELLED,
                lastEventId = accumulateEventId(this.lastEventId, lastUpdateAt.toString())
            )
        }

        return this
    }

    private suspend fun Order.withBidExpire(): Order {
        if (this.isBid().not()) return this
        if (this.platform != Platform.RARIBLE) return this
        if (this.status !in EXPIRED_BID_STATUSES) return this

        val now = Instant.now()
        val expiredDate = now - raribleOrderExpiration.bidExpirePeriod

        return if (
        //Bids witch were expired by 'end' time must be canceled also
            this.isEnded() ||
            this.lastUpdateAt <= expiredDate
        ) {
            logger.info("Cancel rarible BID $id cause it expired after $expiredDate")
            this.copy(
                status = OrderStatus.CANCELLED,
                lastUpdateAt = Instant.now(),
                dbUpdatedAt = Instant.now(),
                cancelled = true,
                lastEventId = accumulateEventId(this.lastEventId, "$expiredDate")
            )
        } else this
    }

    private suspend fun Order.withNoExpire(): Order {
        if (this.platform != Platform.RARIBLE) return this
        return if (this.end == null && (isBid() || status == OrderStatus.INACTIVE)) {
            logger.info("Cancel rarible order $id cause it has no 'end' time")
            this.copy(
                status = OrderStatus.CANCELLED,
                lastUpdateAt = Instant.now(),
                dbUpdatedAt = Instant.now(),
                cancelled = true,
                lastEventId = accumulateEventId(this.lastEventId, "")
            )
        } else this
    }

    private suspend fun Order.withFinalState(): Order {
        val state = orderStateRepository.getById(id.hash) ?: return this
        return this.withFinalState(state)
    }

    private suspend fun Order.withApproval(): Order {
        if (this.status !in setOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE)) return this
        return if (this.isBid()) withBidApproval() else withSellApproval()
    }

    private suspend fun Order.withSellApproval(): Order {
        val collection = when {
            make.type.nft -> make.type.token
            else -> return this
        }
        val hasApproved = approveService.checkApprove(maker, collection, platform, approved)
        return if (hasApproved != this.approved) {
            logger.info("Change order $id approval to $hasApproved!")
            this.copy(
                approved = hasApproved,
                lastEventId = accumulateEventId(this.lastEventId, collection.prefixed())
            )
        } else this
    }

    private suspend fun Order.withBidApproval(): Order = this //todo support ERC20 Approve

    private suspend fun updateOrderWithState(orderStub: Order): Order {
        val order = orderStub
            .withUpdatedPoolPrice()
            .withUpdatedMakeStock()
            .withNewPrice()
            .withUpdatedCounter()
            .withCancelOpenSea()
            .withCancelSeaport()
            .withApproval()
            .withBidExpire()
            .withNoExpire()
            .withCancelSmallPriceSeaport()
            .withFinalState()

        val saved = orderRepository.save(order)
        logger.info(buildString {
            append("Updated order: ")
            append("id=${saved.id}, ")
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
            OrderType.LOOKSRARE_V2 -> exchangeContractAddresses.looksrareV2
            OrderType.X2Y2 -> exchangeContractAddresses.x2y2V1
            OrderType.AMM -> (data as OrderAmmData).poolAddress
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
            id = Order.Id(EMPTY_ORDER_HASH)
        )

        private fun accumulateEventId(lastEventId: String?, eventId: String): String {
            return Order.accumulateEventId(lastEventId, eventId)
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
            if (u1 is OrderUpdate.ByExchangeLogEvent && u2 is OrderUpdate.ByExchangeLogEvent) {
                return@r logEventComparator.compare(u1.logEvent, u2.logEvent)
            }
            if (u1 is OrderUpdate.ByOrderVersion && u2 is OrderUpdate.ByExchangeLogEvent) return@r -1
            return@r 1
        }

        private val logEventComparator = compareBy<ReversedEthereumLogRecord> { it.blockNumber ?: 0 } then
            compareBy { it.logIndex ?: 0 } then
            compareBy { it.minorLogIndex }

        val logger: Logger = LoggerFactory.getLogger(OrderReduceService::class.java)

        fun EventData.toExchangeHistory(): OrderExchangeHistory =
            requireNotNull(this as? OrderExchangeHistory) { "Unexpected exchange history type ${this::class}" }

        fun EventData.toPoolHistory(): PoolHistory =
            requireNotNull(this as? PoolHistory) { "Unexpected pool history type ${this::class}" }

        fun OrderExchangeHistory.isOnChainOrder(): Boolean {
            return this is OnChainOrder
        }

        fun PoolHistory.isOnChainAmmOrder(): Boolean {
            return this is PoolCreate
        }
    }
}
