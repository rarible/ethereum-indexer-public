package com.rarible.protocol.order.api.controller

import com.rarible.core.common.convert
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.misc.limit
import com.rarible.protocol.order.api.service.order.OrderBidsService
import com.rarible.protocol.order.core.converters.model.PlatformConverter
import com.rarible.protocol.order.core.model.BidStatus
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.PriceOrderVersionFilter
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.math.BigDecimal
import java.time.OffsetDateTime

@RestController
class OrderBidController(
    private val orderBidsService: OrderBidsService,
    private val conversionService: ConversionService
) : OrderBidControllerApi {

    override suspend fun getBidsByItem(
        contract: String,
        tokenId: String,
        status: List<OrderBidStatusDto>,
        maker: String?,
        platform: PlatformDto?,
        startDate: OffsetDateTime?,
        endDate: OffsetDateTime?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrderBidsPaginationDto> {
        val requestSize = size.limit()
        val priceContinuation = Continuation.parse<Continuation.Price>(continuation)
        val makerAddress = if (maker == null) null else Address.apply(maker)
        val filter = PriceOrderVersionFilter.BidByItem(
            Address.apply(contract),
            EthUInt256.of(tokenId),
            makerAddress,
            PlatformConverter.convert(platform),
            startDate?.toInstant(),
            endDate?.toInstant(),
            requestSize,
            priceContinuation
        )
        val statuses = status.map { conversionService.convert<BidStatus>(it) }
        val orderVersions = orderBidsService.findOrderBids(filter, statuses)
        val nextContinuation =
            if (orderVersions.isEmpty() || orderVersions.size < requestSize) null else toContinuation(orderVersions.last().version)
        val result = OrderBidsPaginationDto(
            orderVersions.map { conversionService.convert<OrderBidDto>(it) },
            nextContinuation
        )
        return ResponseEntity.ok(result)
    }

    private fun toContinuation(orderVersion: OrderVersion): String {
        return Continuation.Price(orderVersion.takePriceUsd ?: BigDecimal.ZERO, orderVersion.hash).toString()
    }
}
