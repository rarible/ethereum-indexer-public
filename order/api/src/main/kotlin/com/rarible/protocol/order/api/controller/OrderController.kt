package com.rarible.protocol.order.api.controller

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
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderFilter
import com.rarible.protocol.order.core.model.OrderFilterAll
import com.rarible.protocol.order.core.model.OrderFilterBidByItem
import com.rarible.protocol.order.core.model.OrderFilterBidByMaker
import com.rarible.protocol.order.core.model.OrderFilterSell
import com.rarible.protocol.order.core.model.OrderFilterSellByCollection
import com.rarible.protocol.order.core.model.OrderFilterSellByItem
import com.rarible.protocol.order.core.model.OrderFilterSellByMaker
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.PriceOrderVersionFilter
import com.rarible.protocol.order.core.service.PrepareTxService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
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
    orderIndexerProperties: OrderIndexerProperties
) : OrderControllerApi {

    private val platformFeaturedFilter = PlatformFeaturedFilter(orderIndexerProperties.featureFlags)

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
            sort = OrderFilter.Sort.LAST_UPDATE_DESC,
            status = listOf(OrderStatusDto.ACTIVE)
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
            sort = OrderFilter.Sort.LAST_UPDATE_DESC,
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
            sort = OrderFilter.Sort.MAKE_PRICE_ASC,
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
        val filter = OrderFilterSellByItem(
            contract = Address.apply(contract),
            tokenId = BigInteger(tokenId),
            maker = safeAddress(maker),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilter.Sort.MAKE_PRICE_ASC,
            status = convertStatus(status),
            currency = currencyId?.let { Address.apply(currencyId) }
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
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
            sort = OrderFilter.Sort.LAST_UPDATE_DESC,
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
        val filter = OrderFilterSellByCollection(
            collection = Address.apply(collection),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilter.Sort.LAST_UPDATE_DESC,
            status = convertStatus(status)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterSellByMaker(
            maker = Address.apply(maker),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilter.Sort.LAST_UPDATE_DESC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByMakerAndByStatus(
        maker: String,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterSellByMaker(
            maker = Address.apply(maker),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilter.Sort.LAST_UPDATE_DESC,
            status = convertStatus(status)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByStatus(
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?,
        sort: OrderSortDto?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterSell(
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = convert(sort),
            status = convertStatus(status)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderBidsByItem(
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterBidByItem(
            contract = Address.apply(contract),
            tokenId = BigInteger(tokenId),
            maker = safeAddress(maker),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilter.Sort.TAKE_PRICE_DESC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderBidsByItemAndByStatus(
        contract: String,
        tokenId: String,
        status: List<OrderStatusDto>,
        maker: String?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        currencyId: String?,
        startDate: Long?,
        endDate: Long?
    ): ResponseEntity<OrdersPaginationDto> {
        val requestSize = limit(size)
        val priceContinuation = Continuation.parse<Continuation.Price>(continuation)
        val makerAddress = if (maker == null) null else Address.apply(maker)
        val originAddress = if (origin == null) null else Address.apply(origin)
        val filter = PriceOrderVersionFilter.BidByItem(
            Address.apply(contract),
            EthUInt256.of(tokenId),
            makerAddress,
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

    override suspend fun getOrderBidsByMaker(
        maker: String,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterBidByMaker(
            maker = Address.apply(maker),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilter.Sort.LAST_UPDATE_DESC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderBidsByMakerAndByStatus(
        maker: String,
        status: List<OrderStatusDto>,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        startDate: Long?,
        endDate: Long?
    ): ResponseEntity<OrdersPaginationDto> {
        val requestSize = limit(size)
        val priceContinuation = Continuation.parse<Continuation.Price>(continuation)
        val makerAddress = Address.apply(maker)
        val originAddress = if (origin == null) null else Address.apply(origin)
        val filter = PriceOrderVersionFilter.BidByMaker(
            makerAddress,
            originAddress,
            safePlatforms(platform).mapNotNull { PlatformConverter.convert(it) },
            startDate?.let { Instant.ofEpochSecond(it) },
            endDate?.let { Instant.ofEpochSecond(it) },
            requestSize,
            priceContinuation
        )
        return searchBids(status, filter, requestSize)
    }

    suspend fun searchBids(
        status: List<OrderStatusDto>,
        filter: PriceOrderVersionFilter,
        requestSize: Int
    ): ResponseEntity<OrdersPaginationDto> {
        val statuses = status.map { BidStatusReverseConverter.convert(it) }
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
            if (result.isEmpty() || result.size < requestSize) null else toContinuation(filter, result.last())

        return OrdersPaginationDto(
            result.map { orderDtoConverter.convert(it) },
            nextContinuation
        )
    }

    private fun toContinuation(legacyFilter: OrderFilter, order: Order): String {
        return (when (legacyFilter.sort) {
            OrderFilter.Sort.LAST_UPDATE_DESC -> {
                Continuation.LastDate(order.lastUpdateAt, order.hash)
            }
            OrderFilter.Sort.LAST_UPDATE_ASC -> {
                Continuation.LastDate(order.lastUpdateAt, order.hash)
            }
            OrderFilter.Sort.TAKE_PRICE_DESC -> {
                Continuation.Price(order.takePriceUsd ?: BigDecimal.ZERO, order.hash)
            }
            OrderFilter.Sort.MAKE_PRICE_ASC -> {
                if (legacyFilter.currency != null) {
                    Continuation.Price(order.makePrice ?: BigDecimal.ZERO, order.hash)
                } else {
                    Continuation.Price(order.makePriceUsd ?: BigDecimal.ZERO, order.hash)
                }
            }
        }).toString()
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
        default: OrderFilter.Sort = OrderFilter.Sort.LAST_UPDATE_DESC
    ): OrderFilter.Sort {
        return source?.let { OrderSortDtoConverter.convert(it) } ?: default
    }

    private fun toContinuation(orderVersion: OrderVersion): String {
        return Continuation.Price(orderVersion.takePrice ?: BigDecimal.ZERO, orderVersion.hash).toString()
    }

    private fun limit(size: Int?): Int = PageSize.ORDER.limit(size)
}
