package com.rarible.protocol.order.api.controller

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderBidStatusDto
import com.rarible.protocol.dto.OrderBidsPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.api.service.order.OrderBidsService
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.converters.dto.BidDtoConverter
import com.rarible.protocol.order.core.converters.model.OrderBidStatusConverter
import com.rarible.protocol.order.core.converters.model.PlatformConverter
import com.rarible.protocol.order.core.converters.model.PlatformFeaturedFilter
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.PriceOrderVersionFilter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

@RestController
class OrderBidController(
    private val orderBidsService: OrderBidsService,
    private val bidDtoConverter: BidDtoConverter,
    orderIndexerProperties: OrderIndexerProperties
) : OrderBidControllerApi {

    private val platformFeaturedFilter = PlatformFeaturedFilter(orderIndexerProperties.featureFlags)

    override suspend fun getBidsByItem(
        contract: String,
        tokenId: String,
        status: List<OrderBidStatusDto>,
        maker: String?,
        origin: String?,
        platform: PlatformDto?,
        startDate: Instant?,
        endDate: Instant?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrderBidsPaginationDto> {
        val requestSize = PageSize.ORDER_BID.limit(size)
        val priceContinuation = Continuation.parse<Continuation.Price>(continuation)
        val makerAddress = if (maker == null) null else Address.apply(maker)
        val originAddress = if (origin == null) null else Address.apply(origin)
        val filter = PriceOrderVersionFilter.BidByItem(
            Address.apply(contract),
            EthUInt256.of(tokenId),
            makerAddress,
            originAddress,
            safePlatforms(platform).mapNotNull { PlatformConverter.convert(it) },
            null,
            startDate,
            endDate,
            requestSize,
            priceContinuation
        )
        val statuses = status.map { OrderBidStatusConverter.convert(it) }
        val orderVersions = orderBidsService.findOrderBids(filter, statuses)
        val nextContinuation =
            if (orderVersions.isEmpty() || orderVersions.size < requestSize) null else toContinuation(orderVersions.last().version)
        val result = OrderBidsPaginationDto(
            orderVersions.map { bidDtoConverter.convert(it) },
            nextContinuation
        )
        return ResponseEntity.ok(result)
    }

    private fun safePlatforms(platform: PlatformDto?): List<PlatformDto> {
        return platformFeaturedFilter.filter(platform)
    }

    private fun toContinuation(orderVersion: OrderVersion): String {
        return Continuation.Price(orderVersion.takePriceUsd ?: BigDecimal.ZERO, orderVersion.hash).toString()
    }

}
