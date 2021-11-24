package com.rarible.protocol.nft.api.controller

import com.rarible.core.common.convert
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.domain.OwnershipContinuation
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.core.page.PageSize
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.math.BigInteger

@RestController
class OwnershipController(
    private val ownershipApiService: OwnershipApiService,
    private val conversionService: ConversionService
) : NftOwnershipControllerApi {

    private val defaultSorting = NftOwnershipFilterDto.Sort.LAST_UPDATE

    override suspend fun getNftAllOwnerships(continuation: String?, size: Int?): ResponseEntity<NftOwnershipsDto> {
        val filter = NftOwnershipFilterAllDto(defaultSorting)
        val result = getItems(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOwnershipById(ownershipId: String): ResponseEntity<NftOwnershipDto> {
        val result = ownershipApiService.get(conversionService.convert(ownershipId))
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftOwnershipsDto> {
        val filter = NftOwnershipFilterByItemDto(
            defaultSorting,
            Address.apply(contract),
            BigInteger(tokenId)
        )
        val result = getItems(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    private suspend fun getItems(filter: NftOwnershipFilterDto, continuation: String?, size: Int?): NftOwnershipsDto {
        val ownerships = ownershipApiService
            .search(filter, continuation?.let { OwnershipContinuation.parse(it) }, PageSize.OWNERSHIP.limit(size))

        val last = if (ownerships.isEmpty()) null else ownerships.last()
        val cont = last?.let { OwnershipContinuation(it.date, it.id) }?.toString()
        return NftOwnershipsDto(
            ownerships.size.toLong(),
            cont,
            ownerships.map { conversionService.convert<NftOwnershipDto>(it) }
        )
    }
}
