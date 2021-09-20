package com.rarible.protocol.nftorder.api.service

import com.rarible.core.common.convert
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nftorder.core.model.ExtendedItem
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.ItemService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ItemApiService(
    private val conversionService: ConversionService,
    private val nftItemControllerApi: NftItemControllerApi,
    private val itemService: ItemService
) {

    private val logger = LoggerFactory.getLogger(ItemApiService::class.java)

    suspend fun getItemById(itemId: ItemId): NftOrderItemDto = coroutineScope {
        logger.debug("Get item: [{}]", itemId)
        val item = itemService.getOrFetchItemById(itemId).entity
        val meta = itemService.fetchItemMetaById(itemId)
        conversionService.convert<NftOrderItemDto>(ExtendedItem(item, meta))
    }

    suspend fun getAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): NftOrderItemsPageDto {
        logger.debug(
            "Get all Items with params: continuation={}, size={}, showDeleted={}, lastUpdatedFrom={}, lastUpdatedTo={}",
            continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        )
        return itemsResponse(
            nftItemControllerApi.getNftAllItems(
                continuation,
                size,
                showDeleted,
                lastUpdatedFrom,
                lastUpdatedTo
            )
        )
    }

    suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): NftOrderItemsPageDto {
        logger.debug(
            "Get Items by owner with params: owner=[{}], continuation={}, size={}",
            owner, continuation, size
        )
        return itemsResponse(nftItemControllerApi.getNftItemsByOwner(owner, continuation, size))
    }

    suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?
    ): NftOrderItemsPageDto {
        logger.debug(
            "Get Items by creator with params: creator=[{}], continuation={}, size={}",
            creator, continuation, size
        )
        return itemsResponse(nftItemControllerApi.getNftItemsByCreator(creator, continuation, size))
    }

    suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): NftOrderItemsPageDto {
        logger.debug(
            "Get Items by collection with params: collection=[{}], continuation={}, size={}",
            collection, continuation, size
        )
        return itemsResponse(
            nftItemControllerApi.getNftItemsByCollection(collection, continuation, size)
        )
    }

    suspend fun getLazyItemById(itemId: String): LazyNftDto {
        logger.debug("Get LazyItem: itemId=[{}]", itemId)
        return nftItemControllerApi.getNftLazyItemById(itemId).awaitFirst()
    }

    suspend fun getItemMetaById(itemId: String): NftItemMetaDto {
        logger.debug("Get ItemMeta: itemId=[{}]", itemId)
        return nftItemControllerApi.getNftItemMetaById(itemId).awaitFirst()
    }

    private suspend fun itemsResponse(itemsDto: Mono<NftItemsDto>): NftOrderItemsPageDto {
        val itemsResponse = itemsDto.awaitFirst()
        val items = itemsResponse.items
        return if (items.isEmpty()) {
            logger.debug("No Items found")
            NftOrderItemsPageDto(null, emptyList())
        } else {
            val existingItems: Map<ItemId, Item> = itemService
                .findAll(items.map { ItemId.parseId(it.id) })
                .associateBy { it.id }
            logger.debug("{} enriched of {} Items found in DB", existingItems.size, items.size)

            val result = items.map {
                val itemId = ItemId.parseId(it.id)
                // Nothing to enrich, taking item we got from Nft-Indexer
                val item = existingItems[itemId]
                    ?.let { existingItem -> ExtendedItem(existingItem, it.meta) }
                    ?: conversionService.convert(it)

                conversionService.convert<NftOrderItemDto>(item)
            }
            NftOrderItemsPageDto(itemsResponse.continuation, result)
        }
    }

}
