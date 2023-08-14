package com.rarible.protocol.nft.api.controller

import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipIdsDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.dto.parser.parse
import com.rarible.protocol.nft.api.converter.OwnershipIdConverter
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.core.converters.dto.OwnershipDtoConverter
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterAll
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
import com.rarible.protocol.nft.core.model.OwnershipFilterByOwner
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.page.PageSize
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger

@RestController
class OwnershipController(
    private val ownershipApiService: OwnershipApiService
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

    override suspend fun getNftOwnershipById(
        ownershipId: String,
        showDeleted: Boolean?
    ): ResponseEntity<NftOwnershipDto> {
        val safeShowDeleted = showDeleted ?: false
        val result = ownershipApiService.get(OwnershipIdConverter.convert(ownershipId), safeShowDeleted)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOwnershipsByIds(nftOwnershipIdsDto: NftOwnershipIdsDto): ResponseEntity<NftOwnershipsDto> {
        val result = ownershipApiService.get(nftOwnershipIdsDto.ids.map(OwnershipId::parseId))
        return ResponseEntity.ok(
            NftOwnershipsDto(total = result.size.toLong(), continuation = null, ownerships = result)
        )
    }

    override suspend fun getNftOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftOwnershipsDto> {
        val filter = OwnershipFilterByItem(
            defaultSorting,
            AddressParser.parse(contract),
            BigInteger(tokenId)
        )
        val result = getItems(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOwnershipsByOwner(
        owner: String,
        collection: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftOwnershipsDto> {
        val filter = OwnershipFilterByOwner(defaultSorting, AddressParser.parse(owner), AddressParser.parse(collection))
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
            ownerships.map { OwnershipDtoConverter.convert(it) }
        )
    }
}
