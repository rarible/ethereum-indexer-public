package com.rarible.protocol.nft.api.controller

import com.rarible.core.common.convert
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterAll
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
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

    private val defaultSorting = OwnershipFilter.Sort.LAST_UPDATE

    override suspend fun getNftAllOwnerships(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?
    ): ResponseEntity<NftOwnershipsDto> {
        val filter = OwnershipFilterAll(defaultSorting, showDeleted ?: false)
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
        val filter = OwnershipFilterByItem(
            defaultSorting,
            Address.apply(contract),
            BigInteger(tokenId)
        )
        val result = getItems(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    private suspend fun getItems(filter: OwnershipFilter, continuation: String?, size: Int?): NftOwnershipsDto {
        val requestLimit = PageSize.OWNERSHIP.limit(size)
        val ownerships = ownershipApiService.search(filter, continuation?.let { OwnershipContinuation.parse(it) }, requestLimit)
        val last = if (ownerships.isEmpty() || ownerships.size < requestLimit) null else ownerships.last()
        val cont = last?.let { OwnershipContinuation(it.date, it.id) }?.toString()
        return NftOwnershipsDto(
            ownerships.size.toLong(),
            cont,
            ownerships.map { conversionService.convert(it) }
        )
    }
}
