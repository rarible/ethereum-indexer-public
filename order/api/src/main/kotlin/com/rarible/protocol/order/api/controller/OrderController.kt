package com.rarible.protocol.order.api.controller

import com.rarible.core.common.convert
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.api.exceptions.InvalidParameterException
import com.rarible.protocol.order.api.misc.limit
import com.rarible.protocol.order.core.converters.model.AssetConverter

import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.service.OrderInvertService
import com.rarible.protocol.order.core.converters.dto.AssetDtoConverter
import com.rarible.protocol.order.core.converters.model.PartConverter
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.PrepareTxService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.core.convert.ConversionService
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
    private val orderInvertService: OrderInvertService,
    private val conversionService: ConversionService,
    private val prepareTxService: PrepareTxService
) : OrderControllerApi {

    override suspend fun invertOrder(
        hash: String,
        form: InvertOrderFormDto
    ): ResponseEntity<OrderFormDto> {
        val order = orderService.get(Word.apply(hash))
        val inverted = orderInvertService.invert(order, form.maker, form.amount, form.salt.toWord(), convert(form.originFees))
        return ResponseEntity.ok(conversionService.convert(inverted))
    }

    override suspend fun prepareOrderV2Transaction(
        hash: String,
        form: OrderFormDto
    ): ResponseEntity<PrepareOrderTxResponseDto> {
        val order = orderService.get(Word.apply(hash))
        val orderRight = orderService.convertForm(form)
        val result = with(prepareTxService.prepareTxFor2Orders(order, orderRight)) {
            PrepareOrderTxResponseDto(
                transferProxyAddress,
                AssetDtoConverter.convert(asset),
                PreparedOrderTxDto(transaction.to, transaction.data)
            )
        }
        return ResponseEntity.ok(result)
    }

    override suspend fun prepareOrderTransaction(
        hash: String,
        form: PrepareOrderTxFormDto
    ): ResponseEntity<PrepareOrderTxResponseDto> {
        val order = orderService.get(Word.apply(hash))
        val result = with(prepareTxService.prepareTransaction(order, form)) {
            PrepareOrderTxResponseDto(
                transferProxyAddress,
                AssetDtoConverter.convert(asset),
                PreparedOrderTxDto(transaction.to, transaction.data)
            )
        }
        return ResponseEntity.ok(result)
    }

    override suspend fun buyerFeeSignature(fee: Int, orderFormDto: OrderFormDto): ResponseEntity<Binary> {
        val data = when (orderFormDto) {
            is LegacyOrderFormDto -> orderFormDto.data
            else -> throw InvalidParameterException("Unsupported order form type, should use only LegacyOrderForm type")
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
        val result = conversionService.convert<OrderDto>(order)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderByHash(hash: String): ResponseEntity<OrderDto> {
        val order = orderService.get(Word.apply(hash))
        val result = conversionService.convert<OrderDto>(order)
        return ResponseEntity.ok(result)
    }

    override suspend fun updateOrderMakeStock(
        hash: String
    ): ResponseEntity<OrderDto> {
        val order = orderService.updateMakeStock(Word.apply(hash))
        val result = conversionService.convert<OrderDto>(order)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrdersAll(
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterAllDto(
            origin = safeAddress(origin),
            platform = platform,
            sort = OrderFilterDto.Sort.LAST_UPDATE
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
        val filter = OrderFilterSellDto(
            origin = safeAddress(origin),
            platform = platform,
            sort = OrderFilterDto.Sort.LAST_UPDATE
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
        val filter = OrderFilterSellByItemDto(
            contract = Address.apply(contract),
            tokenId = BigInteger(tokenId),
            maker = safeAddress(maker),
            origin = safeAddress(origin),
            platform = platform,
            sort = OrderFilterDto.Sort.MAKE_PRICE_ASC
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
        val filter = OrderFilterSellByCollectionDto(
            collection = Address.apply(collection),
            origin = safeAddress(origin),
            platform = platform,
            sort = OrderFilterDto.Sort.LAST_UPDATE
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
        val filter = OrderFilterSellByMakerDto(
            maker = Address.apply(maker),
            origin = safeAddress(origin),
            platform = platform,
            sort = OrderFilterDto.Sort.LAST_UPDATE
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
        val filter = OrderFilterBidByItemDto(
            contract = Address.apply(contract),
            tokenId = BigInteger(tokenId),
            maker = safeAddress(maker),
            origin = safeAddress(origin),
            platform = platform,
            sort = OrderFilterDto.Sort.TAKE_PRICE_DESC
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOrderBidsByMaker(
        maker: String,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersPaginationDto> {
        val filter = OrderFilterBidByMakerDto(
            maker = Address.apply(maker),
            origin = safeAddress(origin),
            platform = platform,
            sort = OrderFilterDto.Sort.LAST_UPDATE
        )
        val result = searchOrders(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    private suspend fun searchOrders(
        filter: OrderFilterDto,
        continuation: String?,
        size: Int?
    ): OrdersPaginationDto {
        val requestSize = size.limit()
        val result = orderService.findOrders(filter, requestSize, continuation)
        val nextContinuation =
            if (result.isEmpty() || result.size < requestSize) null else toContinuation(filter, result.last())

        return OrdersPaginationDto(
            result.map { conversionService.convert<OrderDto>(it) },
            nextContinuation
        )
    }

    private fun toContinuation(filter: OrderFilterDto, order: Order): String {
        return when (filter.sort) {
            OrderFilterDto.Sort.LAST_UPDATE -> {
                Continuation.LastDate(order.lastUpdateAt, order.hash)
            }
            OrderFilterDto.Sort.TAKE_PRICE_DESC -> {
                Continuation.Price(order.takePriceUsd ?: BigDecimal.ZERO, order.hash)
            }
            OrderFilterDto.Sort.MAKE_PRICE_ASC -> {
                Continuation.Price(order.makePriceUsd ?: BigDecimal.ZERO, order.hash)
            }
        }.toString()
    }

    private fun safeAddress(value: String?): Address? {
        return if (value == null) null else Address.apply(value)
    }

    private fun convert(source: List<PartDto>): List<Part> {
        return source.map { PartConverter.convert(it) }
    }
}
