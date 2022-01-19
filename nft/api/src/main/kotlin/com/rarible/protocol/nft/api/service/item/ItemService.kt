package com.rarible.protocol.nft.api.service.item

import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.convert
import com.rarible.core.common.mapAsync
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemRoyaltyDto
import com.rarible.protocol.dto.NftItemRoyaltyListDto
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.service.descriptor.RoyaltyCacheDescriptor
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.core.model.ItemFilter
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByOwner
import com.rarible.protocol.nft.core.page.PageSize
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ItemService(
    private val conversionService: ConversionService,
    private val itemMetaService: ItemMetaService,
    private val royaltyCacheDescriptor: RoyaltyCacheDescriptor,
    private val cacheService: CacheService,
    private val itemRepository: ItemRepository,
    private val ownershipApiService: OwnershipApiService,
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository
) {
    suspend fun getWithAvailableMeta(itemId: ItemId): NftItemDto {
        val item = itemRepository
            .findById(itemId).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("Item", itemId)
        val itemMeta = itemMetaService.getAvailableMetaOrScheduleLoading(itemId)
        val extendedItem = ExtendedItem(item, itemMeta)
        return conversionService.convert(extendedItem)
    }

    suspend fun getLazy(itemId: ItemId): LazyNftDto {
        return lazyNftItemHistoryRepository
            .findLazyMintById(itemId).awaitFirstOrNull()
            ?.let { conversionService.convert<LazyNftDto>(it) }
            ?: throw EntityNotFoundApiException("Lazy Item", itemId)
    }

    suspend fun getRoyalty(itemId: ItemId): NftItemRoyaltyListDto = coroutineScope {
        val parts = cacheService
            .get(itemId.toString(), royaltyCacheDescriptor, true)
            .awaitSingle()
        NftItemRoyaltyListDto(parts.map { NftItemRoyaltyDto(it.account, it.value) })
    }

    suspend fun search(
        filter: ItemFilter,
        continuation: ItemContinuation?,
        size: Int?
    ): List<ExtendedItem> = coroutineScope {
        val requestSize = PageSize.ITEM.limit(size)
        val items = itemRepository.search(filter.toCriteria(continuation, requestSize))
        items.mapAsync { item ->
            val meta = itemMetaService.getAvailableMetaOrScheduleLoading(item.id)
            ExtendedItem(item, meta)
        }
    }

    suspend fun searchByOwner(
        owner: Address,
        continuation: OwnershipContinuation?,
        size: Int
    ): List<ExtendedItem> = coroutineScope {
        val ownershipFilter = OwnershipFilterByOwner(OwnershipFilter.Sort.LAST_UPDATE, owner)
        val ownerships = ownershipApiService.search(ownershipFilter, continuation, size)
            .associate { ItemId(it.token, it.tokenId) to it.date }

        val items = itemRepository.searchByIds(ownerships.keys)
        items.mapAsync { item ->
            val meta = itemMetaService.getAvailableMetaOrScheduleLoading(item.id)

            // We need to replace item's date with ownership's date due to correct ordering
            val date = ownerships[item.id] ?: item.date
            ExtendedItem(item.copy(date = date), meta)
        }.sortedBy { it.item.date }.reversed()
    }

    suspend fun search(list: Set<ItemId>): List<ExtendedItem> = coroutineScope {
        val items = itemRepository.searchByIds(list)
        items.mapAsync { item ->
            val meta = itemMetaService.getAvailableMetaOrScheduleLoading(item.id)
            ExtendedItem(item, meta)
        }
    }
}
