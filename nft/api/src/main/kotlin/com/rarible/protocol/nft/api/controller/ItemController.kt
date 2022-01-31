package com.rarible.protocol.nft.api.controller

import com.rarible.core.common.convert
import com.rarible.core.logging.withMdc
import com.rarible.protocol.dto.BurnLazyNftFormDto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemRoyaltyListDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.api.service.item.ItemService
import com.rarible.protocol.nft.api.service.mint.BurnLazyNftValidator
import com.rarible.protocol.nft.api.service.mint.MintService
import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.ItemFilter
import com.rarible.protocol.nft.core.model.ItemFilterAll
import com.rarible.protocol.nft.core.model.ItemFilterByCollection
import com.rarible.protocol.nft.core.model.ItemFilterByCreator
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.page.PageSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.time.Instant

@ExperimentalCoroutinesApi
@RestController
class ItemController(
    private val itemService: ItemService,
    private val mintService: MintService,
    private val conversionService: ConversionService,
    private val burnLazyNftValidator: BurnLazyNftValidator
) : NftItemControllerApi {

    private val defaultSorting = ItemFilter.Sort.LAST_UPDATE_DESC

    override suspend fun getNftItemById(itemId: String): ResponseEntity<NftItemDto> {
        val result = itemService.get(conversionService.convert(itemId))
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftLazyItemById(itemId: String): ResponseEntity<LazyNftDto> {
        val result = itemService.getLazy(conversionService.convert(itemId))
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemMetaById(itemId: String): ResponseEntity<NftItemMetaDto> {
        val result = withContext(NonCancellable) {
            itemService.getMeta(conversionService.convert(itemId))
        }
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemRoyaltyById(itemId: String): ResponseEntity<NftItemRoyaltyListDto> {
        val result = itemService.getRoyalty(conversionService.convert(itemId))
        return ResponseEntity.ok(result)
    }

    override suspend fun resetNftItemMetaById(itemId: String): ResponseEntity<Unit> {
        itemService.resetMeta(conversionService.convert(itemId))
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
        @PathVariable("itemId") itemId: kotlin.String,
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
        val ownershipContinuation = continuation?.let { c ->
            ItemContinuation.parse(c)?.let {
                OwnershipContinuation(it.afterDate, OwnershipId(it.afterId.token, it.afterId.tokenId, Address.apply(owner)))
            }
        }
        val requestSize = PageSize.ITEM.limit(size)
        val result = itemService.searchByOwner(Address.apply(owner), ownershipContinuation, requestSize)
        return ResponseEntity.ok(result2Dto(result, requestSize))
    }

    override suspend fun getNftItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftItemsDto> {
        val filter = ItemFilterByCreator(
            defaultSorting,
            Address.apply(creator)
        )

        val result = getItems(filter, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftItemsDto> {
        val filter = ItemFilterByCollection(
            defaultSorting,
            Address.apply(collection),
            owner?.let { Address.apply(it) }
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
        val result = itemService.search(filter, ItemContinuation.parse(continuation), requestSize)
        return result2Dto(result, requestSize)
    }

    private fun result2Dto(result: List<ExtendedItem>, requestSize: Int): NftItemsDto {
        val last = if (result.isEmpty() || result.size < requestSize) null else result.last()
        val cont = last?.let { ItemContinuation(it.item.date, it.item.id) }?.toString()
        val itemsDto = result.map { conversionService.convert<NftItemDto>(it) }
        return NftItemsDto(itemsDto.size.toLong(), cont, itemsDto)
    }

    companion object {
        const val BURN_MSG = "I would like to burn my %s item."
    }
}
