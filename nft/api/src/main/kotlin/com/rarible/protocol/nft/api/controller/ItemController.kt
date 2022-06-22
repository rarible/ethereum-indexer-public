package com.rarible.protocol.nft.api.controller

import com.rarible.core.common.convert
import com.rarible.core.logging.RaribleMDCContext
import com.rarible.core.logging.withMdc
import com.rarible.protocol.dto.BurnLazyNftFormDto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemIdsDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemRoyaltyListDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.service.item.ItemService
import com.rarible.protocol.nft.api.service.mint.BurnLazyNftValidator
import com.rarible.protocol.nft.api.service.mint.MintService
import com.rarible.protocol.nft.core.converters.dto.NftItemMetaDtoConverter
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.core.model.ItemFilter
import com.rarible.protocol.nft.core.model.ItemFilterAll
import com.rarible.protocol.nft.core.model.ItemFilterByCollection
import com.rarible.protocol.nft.core.model.ItemFilterByCreator
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.page.PageSize
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant

@ExperimentalCoroutinesApi
@RestController
class ItemController(
    private val itemService: ItemService,
    private val itemMetaService: ItemMetaService,
    private val mintService: MintService,
    private val conversionService: ConversionService,
    private val burnLazyNftValidator: BurnLazyNftValidator,
    private val nftIndexerApiProperties: NftIndexerApiProperties,
    private val nftItemMetaDtoConverter: NftItemMetaDtoConverter
) : NftItemControllerApi {

    override suspend fun getNftItemById(itemId: String): ResponseEntity<NftItemDto> {
        val result = itemService
            .getById(conversionService.convert(itemId))
            .let { conversionService.convert<NftItemDto>(it) }
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftLazyItemById(itemId: String): ResponseEntity<LazyNftDto> {
        val result = itemService
            .getLazyById(conversionService.convert(itemId))
            .let { conversionService.convert<LazyNftDto>(it) }
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemMetaById(itemId: String): ResponseEntity<NftItemMetaDto> {
        val availableMeta = itemMetaService.getAvailableMetaWithTimeout(
            itemId = conversionService.convert(itemId),
            timeout = Duration.ofMillis(nftIndexerApiProperties.metaSyncLoadingTimeout),
            demander = "get meta by ID"
        ) ?: throw EntityNotFoundApiException("Item meta", itemId)
        return ResponseEntity.ok(nftItemMetaDtoConverter.convert(availableMeta, itemId))
    }

    override suspend fun getNftItemRoyaltyById(itemId: String): ResponseEntity<NftItemRoyaltyListDto> {
        logger.debug("Got request to get nft item royalty by id, parameter: $itemId")
        val convertedItemId: ItemId = conversionService.convert(itemId)
        logger.debug("ItemId: $itemId was converted to: $convertedItemId")
        val result = itemService.getRoyalty(convertedItemId)
        return ResponseEntity.ok(result)
    }

    override suspend fun resetNftItemMetaById(itemId: String): ResponseEntity<Unit> {
        // TODO Remove in PT-568

        return ResponseEntity.noContent().build()
    }

    override suspend fun deleteLazyMintNftAsset(
        itemId: String,
        burnLazyNftFormDto: BurnLazyNftFormDto
    ): ResponseEntity<Unit> {
        return deleteLazyMintNftAssetInternal(itemId, burnLazyNftFormDto)
    }

    @Deprecated("Hidden in release 1.17, should be completely removed later")
    @DeleteMapping(
        value = ["/v0.1/items/{itemId}/lazy"],
        produces = ["application/json"],
        consumes = ["application/json"]
    )
    suspend fun deleteLazyMintNftAssetDeprecated(
        @PathVariable("itemId") itemId: String,
        @RequestBody burnLazyNftFormDto: BurnLazyNftFormDto
    ): ResponseEntity<Unit> {
        return withMdc { deleteLazyMintNftAssetInternal(itemId, burnLazyNftFormDto) }
    }

    private suspend fun deleteLazyMintNftAssetInternal(
        itemId: String,
        burnLazyNftFormDto: BurnLazyNftFormDto
    ): ResponseEntity<Unit> {
        val item: ItemId = conversionService.convert(itemId)
        burnLazyNftValidator.validate(item, BURN_MSG.format(item.tokenId.value), burnLazyNftFormDto)
        mintService.burnLazyMint(item)
        return ResponseEntity.noContent().build()
    }

    override suspend fun getNftAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ResponseEntity<NftItemsDto> {
        val filter = ItemFilterAll(
            defaultSorting,
            showDeleted ?: false,
            lastUpdatedFrom?.let { Instant.ofEpochMilli(it) }
        )
        val filterContinuation = continuation ?: lastUpdatedTo?.let {
            ItemContinuation(Instant.ofEpochMilli(it), ItemId.MAX_ID).toString()
        }

        val result = getItems(filter, filterContinuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftItemsDto> {
        val ownerAddress = AddressParser.parse(owner)
        val ownershipContinuation = continuation?.let { c ->
            ItemContinuation.parse(c)?.let {
                OwnershipContinuation(
                    it.afterDate,
                    OwnershipId(it.afterId.token, it.afterId.tokenId, ownerAddress)
                )
            }
        }
        val requestSize = PageSize.ITEM.limit(size)
        val result = itemService.searchByOwner(
            owner = ownerAddress,
            continuation = ownershipContinuation,
            size = requestSize
        )
        return ResponseEntity.ok(result2Dto(result, requestSize))
    }

    override suspend fun getNftItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftItemsDto> {
        val filter = ItemFilterByCreator(
            defaultSorting,
            AddressParser.parse(creator)
        )
        val result = getItems(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override fun getNftItemsByIds(nftItemIdsDto: NftItemIdsDto): ResponseEntity<Flow<NftItemDto>> {
        val ids = nftItemIdsDto.ids.map { ItemId.parseId(it) }.toSet()
        val items = flow<NftItemDto> {
            itemService.search(ids = ids)
                .forEach { emit(conversionService.convert(it)) }
        }.flowOn(RaribleMDCContext())
        return ResponseEntity.ok(items)
    }

    override suspend fun getNftItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftItemsDto> {
        val filter = ItemFilterByCollection(
            sort = defaultSorting,
            collection = AddressParser.parse(collection),
            owner = owner?.let { AddressParser.parse(it) }
        )
        val result = getItems(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    private suspend fun getItems(
        filter: ItemFilter,
        continuation: String?,
        size: Int?
    ): NftItemsDto {
        val requestSize = PageSize.ITEM.limit(size)
        val result = itemService.search(
            filter = filter,
            continuation = ItemContinuation.parse(continuation),
            size = requestSize
        )
        return result2Dto(result, requestSize)
    }

    private fun result2Dto(result: List<Item>, requestSize: Int): NftItemsDto {
        val last = if (result.isEmpty() || result.size < requestSize) null else result.last()
        val cont = last?.let { ItemContinuation(it.date, it.id) }?.toString()
        val itemsDto = result.map { conversionService.convert<NftItemDto>(it) }
        return NftItemsDto(itemsDto.size.toLong(), cont, itemsDto)
    }

    companion object {
        const val BURN_MSG = "I would like to burn my %s item."
        private val logger = LoggerFactory.getLogger(ItemController::class.java)
        private val defaultSorting = ItemFilter.Sort.LAST_UPDATE_DESC
    }
}
