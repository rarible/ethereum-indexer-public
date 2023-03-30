package com.rarible.protocol.nft.api.controller

import com.rarible.protocol.dto.CollectionsByIdRequestDto
import com.rarible.protocol.dto.EthCollectionMetaResultDto
import com.rarible.protocol.dto.EthMetaStatusDto
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionStatsDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.dto.NftTokenIdDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties
import com.rarible.protocol.nft.api.converter.MetaStatusConverter
import com.rarible.protocol.nft.api.service.colllection.CollectionService
import com.rarible.protocol.nft.core.converters.dto.EthCollectionMetaDtoConverter
import com.rarible.protocol.nft.core.converters.dto.ExtendedCollectionDtoConverter
import com.rarible.protocol.nft.core.converters.dto.TokenIdDtoConverter
import com.rarible.protocol.nft.core.model.TokenFilter
import com.rarible.protocol.nft.core.page.PageSize
import com.rarible.protocol.nft.core.service.CollectionStatService
import com.rarible.protocol.nft.core.service.item.meta.MetaException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.time.Duration
import java.time.Instant

@RestController
class CollectionController(
    private val collectionService: CollectionService,
    private val collectionStatService: CollectionStatService,
    private val nftIndexerApiProperties: NftIndexerApiProperties
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

    override suspend fun getNftCollectionsByIds(collectionsByIdRequestDto: CollectionsByIdRequestDto): ResponseEntity<NftCollectionsDto> {
        val collections = collectionService.get(collectionsByIdRequestDto.ids.map { AddressParser.parse(it) })
        return ResponseEntity.ok(
            NftCollectionsDto(
                total = collections.size.toLong(),
                collections = collections,
                continuation = null
            )
        )
    }

    override suspend fun resetNftCollectionMetaById(collection: String): ResponseEntity<Unit> {
        // TODO Remove in PT-568
        collectionService.resetMeta(AddressParser.parse(collection))
        return ResponseEntity.noContent().build()
    }

    override suspend fun getCollectionMeta(collection: String): ResponseEntity<EthCollectionMetaResultDto> {
        val response = try {
            val meta = collectionService.getMetaWithTimeout(
                address = AddressParser.parse(collection),
                timeout = Duration.ofMillis(nftIndexerApiProperties.metaSyncLoadingTimeout)
            )
            EthCollectionMetaResultDto(
                meta = EthCollectionMetaDtoConverter.convert(meta),
                status = EthMetaStatusDto.OK
            )
        } catch (e: MetaException) {
            EthCollectionMetaResultDto(status = MetaStatusConverter.convert(e.status))
        } catch (e: Throwable) {
            EthCollectionMetaResultDto(status = EthMetaStatusDto.ERROR)
        }
        return ResponseEntity.ok(response)
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
        val result = TokenIdDtoConverter.convert(nextTokenId)
        return ResponseEntity.ok(result)
    }

    private suspend fun searchCollections(filter: TokenFilter): NftCollectionsDto {
        val collections = collectionService.search(filter)
        val continuation =
            if (collections.isEmpty() || collections.size < filter.size) null else collections.last().token.id.toString()

        return NftCollectionsDto(
            total = collections.size.toLong(),
            collections = collections.map { ExtendedCollectionDtoConverter.convert(it) },
            continuation = continuation
        )
    }

    private fun limit(size: Int?): Int = PageSize.TOKEN.limit(size)
}
