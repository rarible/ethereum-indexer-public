package com.rarible.protocol.nft.api.controller

import com.rarible.core.common.convert
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionStatsDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.dto.NftTokenIdDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.nft.api.service.colllection.CollectionService
import com.rarible.protocol.nft.core.model.TokenFilter
import com.rarible.protocol.nft.core.page.PageSize
import com.rarible.protocol.nft.core.service.CollectionStatService
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.time.Instant

@RestController
class CollectionController(
    private val collectionService: CollectionService,
    private val conversionService: ConversionService,
    private val collectionStatService: CollectionStatService
) : NftCollectionControllerApi {

    override suspend fun getNftCollectionById(
        collection: String
    ): ResponseEntity<NftCollectionDto> {
        val result = collectionService.get(AddressParser.parse(collection))
        return ResponseEntity.ok(result)
    }

    // TODO remove later
    override suspend fun getNftCollectionStats(collection: String): ResponseEntity<NftCollectionStatsDto> {
        val address = AddressParser.parse(collection)
        collectionService.get(address) // To throw 404 if not found

        val stat = collectionStatService.getOrSchedule(address)

        // Initial stat record, not filled with real data yet
        val result = if (stat.lastUpdatedAt == Instant.EPOCH) {
            NftCollectionStatsDto(token = stat.id)
        } else {
            NftCollectionStatsDto(
                token = stat.id,
                totalItemSupply = stat.totalItemSupply,
                totalOwnerCount = stat.totalOwnerCount,
                lastUpdatedAt = stat.lastUpdatedAt
            )
        }
        return ResponseEntity.ok(result)
    }

    override suspend fun resetNftCollectionMetaById(collection: String): ResponseEntity<Unit> {
        collectionService.resetMeta(AddressParser.parse(collection))
        return ResponseEntity.noContent().build()
    }

    override suspend fun searchNftAllCollections(
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftCollectionsDto> {
        val filter = TokenFilter.All(continuation, limit(size))
        val result = searchCollections(filter)
        return ResponseEntity.ok(result)
    }

    override suspend fun searchNftCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftCollectionsDto> {
        val filter = TokenFilter.ByOwner(
            AddressParser.parse(owner),
            continuation,
            limit(size)
        )
        val result = searchCollections(filter)
        return ResponseEntity.ok(result)
    }

    override suspend fun generateNftTokenId(
        collection: String,
        minter: String
    ): ResponseEntity<NftTokenIdDto> {
        val collectionAddress = AddressParser.parse(collection)
        val minterAddress = Address.apply(minter)
        val nextTokenId = collectionService.generateId(collectionAddress, minterAddress)
        val result = conversionService.convert<NftTokenIdDto>(nextTokenId)
        return ResponseEntity.ok(result)
    }

    private suspend fun searchCollections(filter: TokenFilter): NftCollectionsDto {
        val collections = collectionService.search(filter)
        val continuation =
            if (collections.isEmpty() || collections.size < filter.size) null else collections.last().token.id.toString()

        return NftCollectionsDto(
            total = collections.size.toLong(),
            collections = collections.map { conversionService.convert(it) },
            continuation = continuation
        )
    }

    private fun limit(size: Int?): Int = PageSize.TOKEN.limit(size)
}
