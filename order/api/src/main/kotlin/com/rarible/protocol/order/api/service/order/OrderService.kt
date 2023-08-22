package com.rarible.protocol.order.api.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.exception.EntityNotFoundApiException
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderAmmData
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.PoolNftItemIds
import com.rarible.protocol.order.core.model.PoolTradePrice
import com.rarible.protocol.order.core.model.currency
import com.rarible.protocol.order.core.model.order.OrderFilter
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.core.service.pool.PoolOwnershipService
import com.rarible.protocol.order.core.validator.OrderValidator
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val poolOwnershipService: PoolOwnershipService,
    private val priceUpdateService: PriceUpdateService,
    private val raribleOrderSaveMetric: RegisteredCounter,
    private val poolCurve: PoolCurve,
    private val poolInfoProvider: PoolInfoProvider,
    private val approveService: ApproveService,
    private val coreOrderValidator: OrderValidator,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
) {
    suspend fun convertFormToVersion(form: OrderForm): OrderVersion {
        val platform = Platform.RARIBLE
        val approved = approveService.checkOnChainApprove(form.maker, form.make.type, platform)
        return OrderVersion(
            maker = form.maker,
            make = form.make,
            take = form.take,
            taker = form.taker,
            type = form.type,
            salt = EthUInt256.of(form.salt),
            start = form.start,
            end = form.end,
            data = form.data,
            signature = form.signature,
            platform = platform,
            hash = form.hash,
            approved = approved,
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null,
        ).run { priceUpdateService.withUpdatedAllPrices(this) }
    }

    suspend fun put(form: OrderForm): Order {
        val eventTimeMarks = orderOffchainEventMarks()
        val orderVersion = convertFormToVersion(form)
        return orderUpdateService
            .save(orderVersion, eventTimeMarks)
            .also { raribleOrderSaveMetric.increment() }
    }

    suspend fun get(hash: Word): Order {
        return orderRepository.findById(hash)
            ?: throw EntityNotFoundApiException("Order", hash)
    }

    suspend fun validateAndGet(hash: Word): Order {
        val order = orderRepository.findById(hash)
            ?: throw EntityNotFoundApiException("Order", hash)

        if (!featureFlags.enableOrderValidation) {
            return order
        }

        coreOrderValidator.validate(order)
        return order
    }

    fun getAll(hashes: List<Word>): Flow<Order> {
        return orderRepository.findAll(hashes)
    }

    suspend fun updateMakeStock(hash: Word): Order =
        orderUpdateService.updateMakeStock(hash, null, orderOffchainEventMarks())
            ?: throw EntityNotFoundApiException("Order", hash)

    suspend fun findOrders(legacyFilter: OrderFilter, size: Int, continuation: String?): List<Order> {
        return orderRepository.search(legacyFilter.toQuery(continuation, size))
    }

    suspend fun getAmmOrderHoldItemIds(hash: Word, continuation: String?, size: Int): PoolNftItemIds {
        val (order, data) = getAmmOrder(hash)
        val pollAddress = data.poolAddress
        val collection = when {
            order.make.type.nft -> order.make.type.token
            order.take.type.nft -> order.take.type.token
            else -> throw OrderDataException("AMM order $hash has not nft asset")
        }
        return poolOwnershipService.getPoolItemIds(pollAddress, collection, continuation, size)
    }

    suspend fun getAmmBuyInfo(hash: Word, nftCount: Int): List<PoolTradePrice> {
        val (order, _) = getAmmOrder(hash)
        val info = poolInfoProvider.getPollInfo(order) ?: error("Unexpectedly can't get pool info from $hash")
        val inputValues = poolCurve.getBuyInputValues(
            curve = info.curve,
            spotPrice = info.protocolFee,
            delta = info.delta,
            numItems = nftCount,
            feeMultiplier = info.fee,
            protocolFeeMultiplier = info.protocolFee
        )
        return coroutineScope {
            inputValues.map { inputValue ->
                val value = inputValue.value
                val currency = order.currency
                async {
                    PoolTradePrice(
                        price = value,
                        priceValue = priceUpdateService.getAssetValue(currency, value),
                        priceUsd = priceUpdateService.getAssetUsdValue(currency, value, nowMillis()),
                    )
                }
            }.awaitAll()
        }
    }

    suspend fun getAmmOrdersByItemId(
        contract: Address,
        tokenId: EthUInt256,
        continuation: String?,
        size: Int
    ): List<Order> {
        val result = poolOwnershipService.getPoolHashesByItemId(contract, tokenId)
        return orderRepository.findAll(result).toList()
    }

    private suspend fun getAmmOrder(hash: Word): Pair<Order, OrderAmmData> {
        val order = orderRepository.findById(hash) ?: throw EntityNotFoundApiException("Order", hash)
        if (order.type != OrderType.AMM) throw OrderDataException("Order $hash type is not AMM")
        if (order.data !is OrderAmmData) throw OrderDataException("Order $hash data is no AMM")
        return order to order.data as OrderAmmData
    }
}
