package com.rarible.protocol.order.api.controller

import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.LegacyOrderFormDto
import com.rarible.protocol.dto.OrderCurrenciesDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.OrderSortDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.dto.PrepareOrderTxResponseDto
import com.rarible.protocol.dto.PreparedOrderTxDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.order.api.exceptions.ValidationApiException
import com.rarible.protocol.order.api.service.order.OrderBidsService
import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.converters.dto.AssetDtoConverter
import com.rarible.protocol.order.core.converters.dto.AssetTypeDtoConverter
import com.rarible.protocol.order.core.converters.dto.BidStatusReverseConverter
import com.rarible.protocol.order.core.converters.dto.CompositeBidConverter
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.converters.model.AssetConverter
import com.rarible.protocol.order.core.converters.model.OrderSortDtoConverter
import com.rarible.protocol.order.core.converters.model.PlatformConverter
import com.rarible.protocol.order.core.converters.model.PlatformFeaturedFilter
import com.rarible.protocol.order.core.converters.model.StatusFeaturedFilter
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.order.OrderFilter
import com.rarible.protocol.order.core.model.order.OrderFilterAll
import com.rarible.protocol.order.core.model.order.OrderFilterBidByItem
import com.rarible.protocol.order.core.model.order.OrderFilterSell
import com.rarible.protocol.order.core.model.order.OrderFilterSellByCollection
import com.rarible.protocol.order.core.model.order.OrderFilterSellByItem
import com.rarible.protocol.order.core.model.order.OrderFilterSellByMaker
import com.rarible.protocol.order.core.model.order.OrderFilterSort
import com.rarible.protocol.order.core.repository.order.BidsOrderVersionFilter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.PrepareTxService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
@RestController
class OrderController(
    private val orderService: OrderService,
    private val orderRepository: OrderRepository,
    private val assetTypeDtoConverter: AssetTypeDtoConverter,
    private val prepareTxService: PrepareTxService,
    private val orderDtoConverter: OrderDtoConverter,
    private val assetDtoConverter: AssetDtoConverter,
    private val orderBidsService: OrderBidsService,
    private val compositeBidConverter: CompositeBidConverter,
    private val orderReduceService: OrderReduceService,
    orderIndexerProperties: OrderIndexerProperties
) : OrderControllerApi {

    private val platformFeaturedFilter = PlatformFeaturedFilter(orderIndexerProperties.featureFlags)
    private val statusFeaturedFilter = StatusFeaturedFilter(orderIndexerProperties.featureFlags)

    @PostMapping(
        value = ["/v0.1/orders/{hash}/reduce"],
        produces = ["application/json"]
    )
    suspend fun reduceOrder(@PathVariable hash: String): OrderDto? {
        val order = optimisticLock {
            orderReduceService.updateOrder(Word.apply(hash))
        }
        return order?.let { orderDtoConverter.convert(it) }
    }

    override suspend fun prepareOrderTransaction(
        hash: String,
        form: PrepareOrderTxFormDto
    ): ResponseEntity<PrepareOrderTxResponseDto> {
        val order = orderService.get(Word.apply(hash))
        val result = with(prepareTxService.prepareTransaction(order, form)) {
            PrepareOrderTxResponseDto(
                transferProxyAddress,
                assetDtoConverter.convert(asset),
                PreparedOrderTxDto(transaction.to, transaction.data)
            )
        }
        return ResponseEntity.ok(result)
    }

    override suspend fun buyerFeeSignature(fee: Int, orderFormDto: OrderFormDto): ResponseEntity<Binary> {
        val data = when (orderFormDto) {
            is LegacyOrderFormDto -> orderFormDto.data
            else -> throw ValidationApiException("Unsupported order form type, should use only LegacyOrderForm type")
        }
        // This is a legacy-support endpoint. Convert only the fields necessary for the calculation of a buyer fee signature.
        val order = Order(
            type = OrderType.RARIBLE_V1,
            data = OrderDataLegacy(data.fee),
            make = AssetConverter.convert(orderFormDto.make),
            take = AssetConverter.convert(orderFormDto.take),
            maker = orderFormDto.maker,
            taker = orderFormDto.taker,
            salt = EthUInt256.of(orderFormDto.salt),

            // Unnecessary fields.
            fill = EthUInt256.ZERO,
            cancelled = false,
            makeStock = EthUInt256.ZERO,
            start = null,
            end = null,
            signature = null,
            createdAt = Instant.EPOCH,
            lastUpdateAt = Instant.EPOCH
        )
        return ResponseEntity.ok(prepareTxService.prepareBuyerFeeSignature(order, fee).toBinary())
    }

    override suspend fun prepareOrderCancelTransaction(hash: String): ResponseEntity<PreparedOrderTxDto> {
        val order = orderService.get(Word.apply(hash))
        val result = with(prepareTxService.prepareCancelTransaction(order)) {
            PreparedOrderTxDto(to, data)
        }
        return ResponseEntity.ok(result)
    }

    override suspend fun upsertOrder(form: OrderFormDto): ResponseEntity<OrderDto> {
        val order = orderService.put(form)
        val result = orderDtoConverter.convert(order)
        return ResponseEntity.ok(result)
    }

    override fun getOrdersByIds(list: OrderIdsDto): ResponseEntity<Flow<OrderDto>> {
        val result = orderService.getAll(list.ids).map { orderDtoConverter.convert(it) }
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderByHash(hash: String): ResponseEntity<OrderDto> {
        val order = orderService.get(Word.apply(hash))
        val result = orderDtoConverter.convert(order)
        return ResponseEntity.ok(result)
    }

    override suspend fun updateOrderMakeStock(
        hash: String
    ): ResponseEntity<OrderDto> {
        val order = orderService.updateMakeStock(Word.apply(hash))
        val result = orderDtoConverter.convert(order)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrdersAll(
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterAll(
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.LAST_UPDATE_DESC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getAllSync(
        sort: SyncSortDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterAll(
            sort = convert(sort),
            platforms = safePlatforms(null)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrdersAllByStatus(
        sort: OrderSortDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterAll(
            sort = convert(sort),
            status = convertStatus(status),
            platforms = safePlatforms(null)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrders(
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterSell(
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.LAST_UPDATE_DESC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterSellByItem(
            contract = Address.apply(contract),
            tokenId = BigInteger(tokenId),
            maker = safeAddress(maker),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.MAKE_PRICE_ASC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByItemAndByStatus(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?,
        currencyId: String?
    ): ResponseEntity<OrdersPaginationDto> {
        return filterInactive(status) { statuses ->
            val filter = OrderFilterSellByItem(
                contract = Address.apply(contract),
                tokenId = BigInteger(tokenId),
                maker = safeAddress(maker),
                origin = safeAddress(origin),
                platforms = safePlatforms(platform),
                sort = OrderFilterSort.MAKE_PRICE_ASC,
                status = convertStatus(statuses),
                currency = currencyId?.let { Address.apply(currencyId) }
            )
            searchOrders(filter, continuation, size)
        }
    }

    override suspend fun getSellOrdersByCollection(
        collection: String,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterSellByCollection(
            collection = Address.apply(collection),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.LAST_UPDATE_DESC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByCollectionAndByStatus(
        collection: String,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersPaginationDto> {
        return filterInactive(status) { statuses ->
            val filter = OrderFilterSellByCollection(
                collection = Address.apply(collection),
                origin = safeAddress(origin),
                platforms = safePlatforms(platform),
                sort = OrderFilterSort.LAST_UPDATE_DESC,
                status = convertStatus(statuses)
            )
            searchOrders(filter, continuation, size)
        }
    }

    override suspend fun getSellOrdersByMakerAndByStatus(
        maker: List<Address>,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersPaginationDto> {
        return filterInactive(status) { statuses ->
            val filter = OrderFilterSellByMaker(
                makers = maker,
                origin = safeAddress(origin),
                platforms = safePlatforms(platform),
                sort = OrderFilterSort.LAST_UPDATE_DESC,
                status = convertStatus(statuses)
            )
            searchOrders(filter, continuation, size)
        }
    }

    override suspend fun getSellOrdersByStatus(
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?,
        sort: OrderSortDto?
    ): ResponseEntity<OrdersPaginationDto> {
        return filterInactive(status) { statuses ->
            val filter = OrderFilterSell(
                origin = safeAddress(origin),
                platforms = safePlatforms(platform),
                sort = convert(sort),
                status = convertStatus(statuses)
            )
            searchOrders(filter, continuation, size)
        }
    }

    override suspend fun getOrderBidsByItem(
        contract: String,
        tokenId: String,
        maker: List<Address>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterBidByItem(
            contract = Address.apply(contract),
            tokenId = BigInteger(tokenId),
            maker = maker,
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.TAKE_PRICE_DESC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderBidsByItemAndByStatus(
        contract: String,
        tokenId: String,
        maker: List<Address>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?,
        currencyId: String?,
        startDate: Long?,
        endDate: Long?
    ): ResponseEntity<OrdersPaginationDto> {
        val requestSize = limit(size)
        val priceContinuation = Continuation.parse<Continuation.Price>(continuation)
        val originAddress = if (origin == null) null else Address.apply(origin)
        val filter = BidsOrderVersionFilter.ByItem(
            Address.apply(contract),
            EthUInt256.of(tokenId),
            maker,
            originAddress,
            safePlatforms(platform).mapNotNull { PlatformConverter.convert(it) },
            currencyId?.let { Address.apply(currencyId) },
            startDate?.let { Instant.ofEpochSecond(it) },
            endDate?.let { Instant.ofEpochSecond(it) },
            requestSize,
            priceContinuation
        )
        return searchBids(status, filter, requestSize)
    }

    override suspend fun getOrderBidsByMakerAndByStatus(
        maker: List<Address>,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?,
        startDate: Long?,
        endDate: Long?
    ): ResponseEntity<OrdersPaginationDto> {
        val requestSize = limit(size)
        val dateContinuation = Continuation.parse<Continuation.LastDate>(continuation)
        val originAddress = if (origin == null) null else Address.apply(origin)
        val filter = BidsOrderVersionFilter.ByMaker(
            maker,
            originAddress,
            safePlatforms(platform).mapNotNull { PlatformConverter.convert(it) },
            startDate?.let { Instant.ofEpochSecond(it) },
            endDate?.let { Instant.ofEpochSecond(it) },
            requestSize,
            dateContinuation
        )
        return searchBids(status, filter, requestSize)
    }

    suspend fun searchBids(
        status: List<OrderStatusDto>?,
        filter: BidsOrderVersionFilter,
        requestSize: Int
    ): ResponseEntity<OrdersPaginationDto> {
        val statuses = status?.map { BidStatusReverseConverter.convert(it) }?.toSet() ?: emptySet()
        val orderVersions = orderBidsService.findOrderBids(filter, statuses)
        val nextContinuation =
            if (orderVersions.isEmpty() || orderVersions.size < requestSize) null else toContinuation(orderVersions.last().version)
        val result = OrdersPaginationDto(
            orderVersions.map { compositeBidConverter.convert(it) },
            nextContinuation
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getCurrenciesBySellOrdersOfItem(
        contract: String,
        tokenId: String
    ): ResponseEntity<OrderCurrenciesDto> {
        val currencies = orderRepository.findTakeTypesOfSellOrders(
            Address.apply(contract),
            EthUInt256.of(BigInteger(tokenId))
        ).map { assetTypeDtoConverter.convert(it) }.toList()
        return ResponseEntity.ok(OrderCurrenciesDto(OrderCurrenciesDto.OrderType.SELL, currencies))
    }

    override suspend fun getCurrenciesByBidOrdersOfItem(
        contract: String,
        tokenId: String
    ): ResponseEntity<OrderCurrenciesDto> {
        val currencies = orderRepository.findMakeTypesOfBidOrders(
            Address.apply(contract),
            EthUInt256.of(BigInteger(tokenId))
        ).map { assetTypeDtoConverter.convert(it) }.toList()
        return ResponseEntity.ok(OrderCurrenciesDto(OrderCurrenciesDto.OrderType.BID, currencies))
    }

    private suspend fun searchOrders(
        filter: OrderFilter,
        continuation: String?,
        size: Int?
    ): OrdersPaginationDto {
        val requestSize = limit(size)

        val result = orderService.findOrders(filter, requestSize, continuation)

        val nextContinuation =
            if (result.isEmpty() || result.size < requestSize) null else filter.toContinuation(result.last())

        return OrdersPaginationDto(
            result.map { orderDtoConverter.convert(it) },
            nextContinuation
        )
    }

    private suspend fun filterInactive(
        statuses: List<OrderStatusDto>?,
        call: suspend (statuses: List<OrderStatusDto>?) -> OrdersPaginationDto
    ): ResponseEntity<OrdersPaginationDto> {
        val result = if (statusFeaturedFilter.emptyResponse(statuses)) {
            OrdersPaginationDto(listOf(), null)
        } else {
            val filtered = statusFeaturedFilter.filter(statuses)
            call(filtered)
        }
        return ResponseEntity.ok(result)
    }

    private fun safeAddress(value: String?): Address? {
        return if (value == null) null else Address.apply(value)
    }

    private fun safePlatforms(platform: PlatformDto?): List<PlatformDto> {
        return platformFeaturedFilter.filter(platform)
    }

    private fun convertStatus(source: List<OrderStatusDto>?): List<OrderStatusDto> {
        return source?.map { OrderStatusDto.valueOf(it.name) } ?: listOf()
    }

    private fun convert(
        source: OrderSortDto?,
        default: OrderFilterSort = OrderFilterSort.LAST_UPDATE_DESC
    ): OrderFilterSort {
        return source?.let { OrderSortDtoConverter.convert(it) } ?: default
    }

    private fun convert(
        source: SyncSortDto?
    ): OrderFilterSort {
        return when (source) {
            SyncSortDto.DB_UPDATE_DESC -> OrderFilterSort.DB_UPDATE_DESC
            else -> OrderFilterSort.DB_UPDATE_ASC
        }
    }

    private fun toContinuation(orderVersion: OrderVersion): String {
        // TODO usage of hash here doesn't work ATM
        return Continuation.Price(orderVersion.takePrice ?: BigDecimal.ZERO, orderVersion.hash).toString()
    }

    private fun limit(size: Int?): Int = PageSize.ORDER.limit(size)
}
