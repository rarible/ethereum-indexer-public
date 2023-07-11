package com.rarible.protocol.order.api.controller

import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AmmTradeInfoDto
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.HoldNftItemIdsDto
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
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.order.api.converter.toAddress
import com.rarible.protocol.order.api.converter.toEthUInt256
import com.rarible.protocol.order.api.service.order.OrderBidsService
import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.converters.dto.AmmTradeInfoDtoConverter
import com.rarible.protocol.order.core.converters.dto.AssetDtoConverter
import com.rarible.protocol.order.core.converters.dto.AssetTypeDtoConverter
import com.rarible.protocol.order.core.converters.dto.BidStatusReverseConverter
import com.rarible.protocol.order.core.converters.dto.CompositeBidConverter
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.converters.model.AssetConverter
import com.rarible.protocol.order.core.converters.model.OrderSortDtoConverter
import com.rarible.protocol.order.core.converters.model.OrderStatusConverter
import com.rarible.protocol.order.core.converters.model.PlatformConverter
import com.rarible.protocol.order.core.converters.model.PlatformFeaturedFilter
import com.rarible.protocol.order.core.exception.ValidationApiException
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Order.Id.Companion.toOrderId
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.currency
import com.rarible.protocol.order.core.model.order.OrderFilter
import com.rarible.protocol.order.core.model.order.OrderFilterAll
import com.rarible.protocol.order.core.model.order.OrderFilterBidByItem
import com.rarible.protocol.order.core.model.order.OrderFilterSell
import com.rarible.protocol.order.core.model.order.OrderFilterSellByCollection
import com.rarible.protocol.order.core.model.order.OrderFilterSellByItem
import com.rarible.protocol.order.core.model.order.OrderFilterSellByMaker
import com.rarible.protocol.order.core.model.order.OrderFilterSort
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.BidsOrderVersionFilter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.core.service.approve.ApproveService
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
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
    private val approveService: ApproveService,
    private val orderUpdateService: OrderUpdateService,
    orderIndexerProperties: OrderIndexerProperties
) : OrderControllerApi {

    private val platformFeaturedFilter = PlatformFeaturedFilter(orderIndexerProperties.featureFlags)

    @PostMapping(
        value = ["/v0.1/orders/{hash}/reduce"],
        produces = ["application/json"]
    )
    suspend fun reduceOrder(@PathVariable hash: String,
                            @RequestParam(name = "withApproval", required = false, defaultValue = "false") withApproval: Boolean): OrderDto? {
        val order = optimisticLock {
            val reduced = orderReduceService.updateOrder(hash.toOrderId().hash)
            if (reduced != null && withApproval) {
                val hasApproved = approveService.checkApprove(reduced.maker, reduced.make.type.token, reduced.platform)
                orderUpdateService.reduceApproval(reduced, hasApproved, orderOffchainEventMarks())
                reduced.copy(approved = hasApproved)
            } else reduced
        }
        return order?.let { orderDtoConverter.convert(it) }
    }

    override suspend fun prepareOrderTransaction(
        hash: String,
        form: PrepareOrderTxFormDto
    ): ResponseEntity<PrepareOrderTxResponseDto> {
        val order = orderService.get(hash.toOrderId().hash)
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
        val order = orderService.get(hash.toOrderId().hash)
        val result = with(prepareTxService.prepareCancelTransaction(order)) {
            PreparedOrderTxDto(to, data)
        }
        return ResponseEntity.ok(result)
    }

    override suspend fun upsertOrder(form: OrderFormDto): ResponseEntity<OrderDto> {
        if (form.end == null || form.end == 0L) {
            throw ValidationApiException("Missed end date")
        }
        val order = orderService.put(form)
        val result = orderDtoConverter.convert(order)
        return ResponseEntity.ok(result)
    }

    override fun getOrdersByIds(list: OrderIdsDto): ResponseEntity<Flow<OrderDto>> {
        val result = orderService.getAll(list.ids.map { it.toOrderId().hash }).map { orderDtoConverter.convert(it) }
        return ResponseEntity.ok(result)
    }

    override suspend fun getByIds(orderIdsDto: OrderIdsDto): ResponseEntity<OrdersPaginationDto> {
        val result = orderService
            .getAll(orderIdsDto.ids.map { it.toOrderId().hash })
            .map { orderDtoConverter.convert(it) }
            .toList()

        return ResponseEntity.ok(OrdersPaginationDto(result))
    }

    override suspend fun getOrderByHash(hash: String): ResponseEntity<OrderDto> {
        val order = orderService.get(hash.toOrderId().hash)
        val result = orderDtoConverter.convert(order)
        return ResponseEntity.ok(result)
    }

    override suspend fun getValidatedOrderByHash(hash: String): ResponseEntity<OrderDto> {
        val order = orderService.validateAndGet(hash.toOrderId().hash)
        val result = orderDtoConverter.convert(order)
        return ResponseEntity.ok(result)
    }

    override suspend fun updateOrderMakeStock(
        hash: String
    ): ResponseEntity<OrderDto> {
        val order = orderService.updateMakeStock(hash.toOrderId().hash)
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

    override suspend fun getAmmBuyInfo(
        hash: String,
        numNFTs: Int
    ): ResponseEntity<AmmTradeInfoDto> {
        val result = orderService.getAmmBuyInfo(hash.toOrderId().hash, numNFTs)
        return ResponseEntity.ok(AmmTradeInfoDtoConverter.convert(result))
    }

    override suspend fun getAmmOrderItemIds(
        hash: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<HoldNftItemIdsDto> {
        val result = orderService.getAmmOrderHoldItemIds(hash.toOrderId().hash, continuation, limit(size))
        return ResponseEntity.ok(HoldNftItemIdsDto(result.itemIds.map { it.toString() }, result.continuation))
    }

    override suspend fun getAmmOrdersByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val requestSize = limit(size)
        val result = orderService.getAmmOrdersByItemId(
            contract = AddressParser.parse(contract),
            tokenId = tokenId.toEthUInt256(),
            continuation = continuation,
            size = limit(size)
        )
        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            result.last().hash.prefixed()
        }
        val dto = OrdersPaginationDto(
            result.map { orderDtoConverter.convert(it) },
            nextContinuation
        )
        return ResponseEntity.ok(dto)
    }

    override suspend fun getOrdersAllByStatus(
        sort: OrderSortDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterAll(
            sort = convert(sort),
            status = safeStatuses(status),
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
            contract = contract.toAddress(),
            tokenId = BigInteger(tokenId),
            maker = safeAddress(maker),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.MAKE_PRICE_ASC,
            status = listOf(OrderStatusDto.ACTIVE)
        )
        val result = searchSellByItemIdOrders(filter, continuation, size)
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
            contract = contract.toAddress(),
            tokenId = BigInteger(tokenId),
            maker = safeAddress(maker),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.MAKE_PRICE_ASC,
            status = safeStatuses(status),
            currency = currencyId?.toAddress()
        )
        val result = searchSellByItemIdOrders(filter, continuation, size)
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
            collection = collection.toAddress(),
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
        val filter = OrderFilterSellByCollection(
            collection = collection.toAddress(),
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.LAST_UPDATE_DESC,
            status = safeStatuses(status)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getSellOrdersByMakerAndByStatus(
        maker: List<Address>,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterSellByMaker(
            makers = maker,
            origin = safeAddress(origin),
            platforms = safePlatforms(platform),
            sort = OrderFilterSort.LAST_UPDATE_DESC,
            status = safeStatuses(status)
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
            status = safeStatuses(status)
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
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
            contract = contract.toAddress(),
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
        val originAddress = origin?.toAddress()
        val filter = BidsOrderVersionFilter.ByItem(
            contract.toAddress(),
            tokenId.toEthUInt256(),
            maker,
            originAddress,
            safePlatforms(platform).mapNotNull { PlatformConverter.convert(it) },
            currencyId?.let { currencyId.toAddress() },
            startDate?.let { Instant.ofEpochSecond(it) },
            endDate?.let { Instant.ofEpochSecond(it) },
            requestSize,
            priceContinuation
        )
        return searchBids(status, filter, requestSize, this::toPriceContinuation)
    }

    override suspend fun getOrderBidsByMakerAndByStatus(
        maker: List<Address>,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?,
        currencyIds: List<Address>?,
        startDate: Long?,
        endDate: Long?
    ): ResponseEntity<OrdersPaginationDto> {
        val requestSize = limit(size)
        val dateContinuation = Continuation.parse<Continuation.LastDate>(continuation)
        val originAddress = origin?.toAddress()
        val filter = BidsOrderVersionFilter.ByMaker(
            maker,
            originAddress,
            safePlatforms(platform).mapNotNull { PlatformConverter.convert(it) },
            currencyIds,
            startDate?.let { Instant.ofEpochSecond(it) },
            endDate?.let { Instant.ofEpochSecond(it) },
            requestSize,
            dateContinuation
        )
        return searchBids(status, filter, requestSize, this::toDateContinuation)
    }

    suspend fun searchBids(
        status: List<OrderStatusDto>?,
        filter: BidsOrderVersionFilter,
        requestSize: Int,
        continuationFactory: (OrderVersion) -> String
    ): ResponseEntity<OrdersPaginationDto> {
        val statuses = status?.map { BidStatusReverseConverter.convert(it) }?.toSet() ?: emptySet()
        val orderVersions = orderBidsService.findOrderBids(filter, statuses)
        val nextContinuation =
            if (orderVersions.isEmpty() || orderVersions.size < requestSize) {
                null
            } else {
                continuationFactory(orderVersions.last().version)
            }
        val result = OrdersPaginationDto(
            orderVersions.map { compositeBidConverter.convert(it) },
            nextContinuation
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getCurrenciesBySellOrdersOfItem(
        contract: String,
        tokenId: String,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrderCurrenciesDto> {
        val sanitizedStatuses = convertCurrencyStatuses(status)
        val currencies = if (sanitizedStatuses.isNotEmpty()) {
            orderRepository.findTakeTypesOfSellOrders(
                contract.toAddress(),
                tokenId.toEthUInt256(),
                sanitizedStatuses
            ).map { assetTypeDtoConverter.convert(it) }.toList()
        } else {
            // Means only HISTORICAL requested, not supported here
            emptyList()
        }
        return ResponseEntity.ok(OrderCurrenciesDto(OrderCurrenciesDto.OrderType.SELL, currencies))
    }

    override suspend fun getCurrenciesByBidOrdersOfItem(
        contract: String,
        tokenId: String,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrderCurrenciesDto> {
        val sanitizedStatuses = convertCurrencyStatuses(status)
        val currencies = if (sanitizedStatuses.isNotEmpty()) {
            orderRepository.findMakeTypesOfBidOrders(
                contract.toAddress(),
                tokenId.toEthUInt256(),
                sanitizedStatuses
            ).map { assetTypeDtoConverter.convert(it) }.toList()
        } else {
            // Means only HISTORICAL requested, not supported here
            emptyList()
        }
        return ResponseEntity.ok(OrderCurrenciesDto(OrderCurrenciesDto.OrderType.BID, currencies))
    }

    private fun convertCurrencyStatuses(dto: List<OrderStatusDto>?): Set<OrderStatus> {
        val statuses = OrderStatusConverter.convert(dto ?: emptyList())
        return if (statuses.isNotEmpty()) {
            statuses.toMutableSet() - OrderStatus.HISTORICAL
        } else {
            OrderStatus.ALL_EXCEPT_HISTORICAL
        }
    }

    // TODO workaround for AMM orders, should be fixed/improved in PT-1652
    private suspend fun searchSellByItemIdOrders(
        filter: OrderFilterSellByItem,
        continuation: String?,
        size: Int?
    ): OrdersPaginationDto {
        val statuses = filter.status ?: emptyList()

        // Works only for ACTIVE and SUDOSWAP orders without origin filter, otherwise AMM orders can't get into result
        val includeSudoswap = (statuses.isEmpty() || statuses.contains(OrderStatusDto.ACTIVE))
            && (filter.platforms.isEmpty() || filter.platforms.contains(PlatformDto.SUDOSWAP))
            && filter.origin == null

        if (!includeSudoswap) {
            return searchOrders(filter, continuation, size)
        }

        // Works only for ERC721, there could be only one AMM order per Item
        val ammOrder = orderService.getAmmOrdersByItemId(
            filter.contract,
            EthUInt256(filter.tokenId),
            null,
            1
        ).firstOrNull() ?: return searchOrders(filter, continuation, size) // Nothing found, using regular search

        val matchesFilter = (filter.maker == null || ammOrder.maker == filter.maker)
            && (filter.currency == null || filter.currency == ammOrder.currency.token)

        // if AMM order found, this page should be the last anyway
        return if (matchesFilter) {
            OrdersPaginationDto(listOf(orderDtoConverter.convert(ammOrder)), null)
        } else {
            OrdersPaginationDto(emptyList(), null)
        }
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

    private fun safeAddress(value: String?): Address? {
        return value?.toAddress()
    }

    private fun safePlatforms(platform: PlatformDto?): List<PlatformDto> {
        return platformFeaturedFilter.filter(platform)
    }

    private fun safeStatuses(source: List<OrderStatusDto>?): List<OrderStatusDto> {
        return source ?: emptyList()
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

    private fun toPriceContinuation(orderVersion: OrderVersion): String {
        // TODO usage of hash here doesn't work ATM
        return Continuation.Price(orderVersion.takePrice ?: BigDecimal.ZERO, orderVersion.hash).toString()
    }

    private fun toDateContinuation(orderVersion: OrderVersion): String {
        // TODO usage of hash here doesn't work ATM
        return Continuation.LastDate(orderVersion.createdAt, orderVersion.hash).toString()
    }

    private fun limit(size: Int?): Int = PageSize.ORDER.limit(size)
}