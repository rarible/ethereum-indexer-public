package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
@RestController
class AuctionController : AuctionControllerApi {
    override suspend fun getAuctionByHash(hash: String): ResponseEntity<AuctionDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAuctionsAll(
        status: List<AuctionStatusDto>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionsPaginationDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAuctionsByCollection(
        collection: String,
        origin: String?,
        status: List<AuctionStatusDto>?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionsPaginationDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAuctionsByItem(
        contract: String,
        tokenId: String,
        seller: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyId: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionsPaginationDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAuctionsBySeller(
        seller: String,
        status: List<AuctionStatusDto>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<AuctionsPaginationDto> {
        TODO("Not yet implemented")
    }
}
