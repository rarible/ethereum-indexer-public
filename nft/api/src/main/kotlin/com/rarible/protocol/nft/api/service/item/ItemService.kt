package com.rarible.protocol.nft.api.service.item

import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.protocol.dto.NftItemRoyaltyDto
import com.rarible.protocol.dto.NftItemRoyaltyListDto
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.service.colllection.CollectionRoyaltiesService
import com.rarible.protocol.nft.api.service.descriptor.RoyaltyCacheDescriptor
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.core.model.ItemFilter
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByOwner
import com.rarible.protocol.nft.core.page.PageSize
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ItemService(
    private val royaltyCacheDescriptor: RoyaltyCacheDescriptor,
    private val collectionRoyaltiesService: CollectionRoyaltiesService,
    private val cacheService: CacheService,
    private val itemRepository: ItemRepository,
    private val ownershipApiService: OwnershipApiService,
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository
) {
    suspend fun getById(itemId: ItemId): Item =
        itemRepository
            .findById(itemId)
            .awaitSingleOrNull() ?: throw EntityNotFoundApiException("Item", itemId)

    suspend fun getLazyById(itemId: ItemId): ItemLazyMint =
        lazyNftItemHistoryRepository
            .findLazyMintById(itemId)
            .awaitFirstOrNull() ?: throw EntityNotFoundApiException("Lazy Item", itemId)

    suspend fun getRoyalty(itemId: ItemId): NftItemRoyaltyListDto = coroutineScope {
        val parts = collectionRoyaltiesService.getRoyaltiesHistory(itemId.token)
            ?: cacheService
                .get(itemId.toString(), royaltyCacheDescriptor, false)
                .awaitSingle()
                .also { logger.debug("Got royalty for item - $ItemId from cache: $it") }
        NftItemRoyaltyListDto(parts.map { NftItemRoyaltyDto(it.account, it.value) })
    }

    suspend fun search(
        filter: ItemFilter,
        continuation: ItemContinuation?,
        size: Int?
    ): List<Item> {
        val requestSize = PageSize.ITEM.limit(size)
        val items = itemRepository.search(filter.toCriteria(continuation, requestSize))
        return items.toList()
    }

    suspend fun searchByOwner(
        owner: Address,
        continuation: OwnershipContinuation?,
        size: Int
    ): List<Item> {
        val ownershipFilter = OwnershipFilterByOwner(OwnershipFilter.Sort.LAST_UPDATE, owner)
        val ownerships = ownershipApiService.search(ownershipFilter, continuation, size)
            .associate { ItemId(it.token, it.tokenId) to it.date }

        return itemRepository.searchByIds(ownerships.keys)
            .map { item ->
                item.copy(
                    // We need to replace item's date with ownership's date due to correct ordering
                    date = ownerships[item.id] ?: item.date
                )
            }
            .sortedBy { it.date }
            .reversed()
    }

    suspend fun getAll(ids: Set<ItemId>): List<Item> = itemRepository.searchByIds(ids)

    companion object {
        private val logger = LoggerFactory.getLogger(ItemService::class.java)
    }
}
