package com.rarible.protocol.order.api.controller

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.dto.parser.BigIntegerParser
import com.rarible.protocol.dto.parser.HashParser
import com.rarible.protocol.order.api.service.auction.AuctionService
import com.rarible.protocol.order.core.continuation.AuctionContinuation
import com.rarible.protocol.order.core.continuation.Continuation
import com.rarible.protocol.order.core.continuation.ContinuationFactory
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.continuation.page.Paging
import com.rarible.protocol.order.core.converters.dto.AuctionBidsDtoConverter
import com.rarible.protocol.order.core.converters.dto.AuctionDtoConverter
import com.rarible.protocol.order.core.converters.model.AuctionSortConverter
import com.rarible.protocol.order.core.converters.model.AuctionStatusConverter
import com.rarible.protocol.order.core.converters.model.PlatformConverter
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.auction.AuctionFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
@RestController
class AuctionController(
    private val auctionService: AuctionService,
    private val auctionDtoConverter: AuctionDtoConverter,
    private val auctionBidsDtoConverter: AuctionBidsDtoConverter
) : AuctionControllerApi {

    override suspend fun getAuctionBidsByHash(
        hash: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionBidsPaginationDto> {
        val safeSize = PageSize.AUCTION_BIDS.limit(size)

        val auctionBids = auctionService
            .getAuctionBids(HashParser.parse(hash), continuation, safeSize)
            .let { auctionBids -> auctionBidsDtoConverter.convert(auctionBids) }

        val page = Paging(AuctionContinuation.ByBidValueAndId, auctionBids).getPage(safeSize).let { page ->
            AuctionBidsPaginationDto(
                bids = page.entities,
                continuation = page.continuation
            )
        }
        return ResponseEntity.ok(page)
    }

    override suspend fun getAuctionByHash(hash: String): ResponseEntity<AuctionDto> {
        val auction = auctionService.get(HashParser.parse(hash))
        return ResponseEntity.ok(convert(auction))
    }

    override suspend fun getAuctionsAll(
        sort: AuctionSortDto?,
        status: List<AuctionStatusDto>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionsPaginationDto> {
        val filter = AuctionFilter.All(
            sort = convert(sort) ?: AuctionFilter.AuctionSort.LAST_UPDATE_DESC,
            origin = convert(origin),
            status = convert(status),
            platform = convert(platform),
            currency = null
        )
        val result = search(filter, size, continuation)
        return ResponseEntity.ok(result)
    }

    override suspend fun getAuctionsByCollection(
        contract: String,
        seller: String?,
        origin: String?,
        status: List<AuctionStatusDto>?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionsPaginationDto> {
        val filter = AuctionFilter.ByCollection(
            token = convertAddress(contract),
            seller = convert(seller),
            sort = AuctionFilter.AuctionSort.LAST_UPDATE_DESC,
            origin = convert(origin),
            status = convert(status),
            platform = convert(platform),
            currency = null
        )
        val result = search(filter, size, continuation)
        return ResponseEntity.ok(result)
    }

    override fun getAuctionsByIds(auctionIdsDto: AuctionIdsDto): ResponseEntity<Flow<AuctionDto>> {
        val result = auctionService.getAll(auctionIdsDto.ids).map { convert(it) }
        return ResponseEntity.ok(result)
    }

    override suspend fun getAuctionsByItem(
        contract: String,
        tokenId: String,
        seller: String?,
        sort: AuctionSortDto?,
        origin: String?,
        status: List<AuctionStatusDto>?,
        currencyId: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionsPaginationDto> {
        val filter = AuctionFilter.ByItem(
            token = convertAddress(contract),
            seller = convert(seller),
            tokenId = convert(tokenId),
            sort = convert(sort) ?: AuctionFilter.AuctionSort.BUY_PRICE_ASC,
            origin = convert(origin),
            status = convert(status),
            platform = convert(platform),
            currency = convert(currencyId)
        )
        val result = search(filter, size, continuation)
        return ResponseEntity.ok(result)
    }

    override suspend fun getAuctionsBySeller(
        seller: String,
        status: List<AuctionStatusDto>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionsPaginationDto> {
        val filter = AuctionFilter.BySeller(
            seller = convertAddress(seller),
            sort = AuctionFilter.AuctionSort.LAST_UPDATE_DESC,
            origin = convert(origin),
            status = convert(status),
            platform = convert(platform),
            currency = null
        )
        val result = search(filter, size, continuation)
        return ResponseEntity.ok(result)
    }

    private suspend fun search(
        filter: AuctionFilter,
        size: Int?,
        continuation: String?
    ): AuctionsPaginationDto {
        val safeSize = PageSize.AUCTION.limit(size)
        val auctions = auctionService.search(filter, safeSize, continuation).map { convert(it) }
        return Paging(convert(filter), auctions).getPage(safeSize).let { page ->
            AuctionsPaginationDto(
                auctions = page.entities,
                continuation = page.continuation
            )
        }
    }

    private fun convertAddress(source: String): Address =
        AddressParser.parse(source)

    private fun convert(source: String): EthUInt256 =
        EthUInt256.of(BigIntegerParser.parse(source))

    private fun convert(source: AuctionSortDto?): AuctionFilter.AuctionSort? =
        source?.let { AuctionSortConverter.convert(source) }

    private fun convert(source: String?): Address? =
        source?.let { AddressParser.parse(source) }

    private fun convert(source: List<AuctionStatusDto>?): List<AuctionStatus>? =
        source?.map { AuctionStatusConverter.convert(it) }

    private fun convert(source: PlatformDto?): List<Platform> =
        source?.let { PlatformConverter.convert(it) }?.let { listOf(it) } ?: emptyList()

    private suspend fun convert(source: Auction): AuctionDto =
        auctionDtoConverter.convert(source)

    private fun convert(filter: AuctionFilter): ContinuationFactory<AuctionDto, Continuation> {
        return when (filter.sort) {
            AuctionFilter.AuctionSort.LAST_UPDATE_ASC,
            AuctionFilter.AuctionSort.LAST_UPDATE_DESC -> {
                AuctionContinuation.ByLastUpdatedAndId
            }
            AuctionFilter.AuctionSort.BUY_PRICE_ASC -> {
                if (filter.currency == null) AuctionContinuation.ByBuyUsdPriceAndId else AuctionContinuation.ByBuyPriceAndId
            }
        }
    }
}
