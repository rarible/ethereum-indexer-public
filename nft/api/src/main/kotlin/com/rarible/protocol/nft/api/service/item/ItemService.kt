package com.rarible.protocol.nft.api.service.item

import com.rarible.core.common.convert
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemFilterDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.nft.api.domain.ItemContinuation
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.service.item.ItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.api.service.item.meta.ItemMetaService
import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemsSearchResult
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class ItemService(
    private val conversionService: ConversionService,
    private val itemMetaService: ItemMetaService,
    private val itemRepository: ItemRepository,
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository
) {
    suspend fun get(itemId: ItemId, includeMeta: Boolean): NftItemDto {
        val item = itemRepository
            .findById(itemId).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("Item ", itemId)

        return if (includeMeta) {
            val meta = itemMetaService.getItemMetadata(itemId)
            conversionService.convert(ExtendedItem(item, meta))
        } else {
            conversionService.convert(item)
        }
    }

    suspend fun getLazy(itemId: ItemId): LazyNftDto {
        return lazyNftItemHistoryRepository
            .findById(itemId).awaitFirstOrNull()
            ?.let { conversionService.convert<LazyNftDto>(it) }
            ?: throw EntityNotFoundApiException("Lazy Item", itemId)
    }

    suspend fun getMeta(itemId: ItemId): NftItemMetaDto {
        return itemMetaService
            .getItemMetadata(itemId)
            .let { conversionService.convert(it) }
    }

    suspend fun resetMeta(itemId: ItemId) {
        itemMetaService.resetMetadata(itemId)
    }

    suspend fun search(
        filter: NftItemFilterDto,
        continuation: ItemContinuation?,
        size: Int?,
        includeMeta: Boolean
    ): ItemsSearchResult = coroutineScope {
        val items = itemRepository.search(filter.toCriteria(continuation, size))

        val meta = if (includeMeta && items.isNotEmpty()) {
            items.map { item ->
                async {
                    val meta = itemMetaService.getItemMetadata(item.id)
                    item.id to meta
                }
            }.awaitAll().toMap()
        } else {
            emptyMap()
        }
        ItemsSearchResult(items, meta)
    }
}
