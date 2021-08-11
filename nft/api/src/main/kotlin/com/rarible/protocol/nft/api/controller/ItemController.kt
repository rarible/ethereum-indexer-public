package com.rarible.protocol.nft.api.controller

import com.rarible.core.common.convert
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.domain.ItemContinuation
import com.rarible.protocol.nft.api.service.item.ItemFilterCriteria.DEFAULT_LIMIT
import com.rarible.protocol.nft.api.service.item.ItemService
import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.time.Instant

@RestController
class ItemController(
    private val itemService: ItemService,
    private val conversionService: ConversionService
) : NftItemControllerApi {

    private val defaultSorting = NftItemFilterDto.Sort.LAST_UPDATE

    override suspend fun getNftItemById(itemId: String, includeMeta: Boolean?): ResponseEntity<NftItemDto> {
        val result = itemService.get(conversionService.convert(itemId), includeMeta.orDefault())
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

    override suspend fun resetNftItemMetaById(itemId: String): ResponseEntity<Unit> {
        itemService.resetMeta(conversionService.convert(itemId))
        return ResponseEntity.noContent().build()
    }

    override suspend fun getNftAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
        includeMeta: Boolean?
    ): ResponseEntity<NftItemsDto> {
        val filter = NftItemFilterAllDto(
            defaultSorting,
            showDeleted ?: false,
            lastUpdatedFrom?.let { Instant.ofEpochMilli(it) }
        )
        val filterContinuation = continuation ?: lastUpdatedTo?.let {
            ItemContinuation(Instant.ofEpochMilli(it), ItemId.MAX_ID).toString()
        }

        val result = getItems(filter, filterContinuation, size, includeMeta)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<NftItemsDto> {
        val filter = NftItemFilterByOwnerDto(
            defaultSorting,
            Address.apply(owner)
        )

        val result = getItems(filter, continuation, size, includeMeta)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<NftItemsDto> {
        val filter = NftItemFilterByCreatorDto(
            defaultSorting,
            Address.apply(creator)
        )

        val result = getItems(filter, continuation, size, includeMeta)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): ResponseEntity<NftItemsDto> {
        val filter = NftItemFilterByCollectionDto(
            defaultSorting,
            Address.apply(collection)
        )

        val result = getItems(filter, continuation, size, includeMeta)
        return ResponseEntity.ok(result)
    }

    private suspend fun getItems(
        filter: NftItemFilterDto,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): NftItemsDto {
        val requestSize = size.limit()
        val (items, metas) = itemService.search(filter, ItemContinuation.parse(continuation), requestSize, includeMeta.orDefault())
        val last = if (items.isEmpty() || items.size < requestSize) null else items.last()
        val cont = last?.let { ItemContinuation(it.date, it.id) }?.toString()

        val itemsDto = items.map { item ->
            val meta = metas[item.id]

            if (meta != null) conversionService.convert<NftItemDto>(ExtendedItem(item, meta))
            else conversionService.convert(item)
        }
        return NftItemsDto(items.size.toLong(), cont, itemsDto)
    }

    companion object {
        fun Boolean?.orDefault(): Boolean = this ?: false
        private fun Int?.limit() = Integer.min(this ?: DEFAULT_LIMIT, DEFAULT_LIMIT)
    }
}