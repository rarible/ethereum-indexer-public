package com.rarible.protocol.nftorder.api.service

import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nftorder.core.converter.ItemToDtoConverter
import com.rarible.protocol.nftorder.core.converter.NftItemDtoToNftOrderItemDtoConverter
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.core.service.OrderService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ItemApiService(
    private val nftItemControllerApi: NftItemControllerApi,
    private val itemService: ItemService,
    private val orderService: OrderService
) {

    private val logger = LoggerFactory.getLogger(ItemApiService::class.java)

    suspend fun getItemById(itemId: ItemId): NftOrderItemDto = coroutineScope {
        logger.debug("Get item: [{}]", itemId)
        val fetchedItem = itemService.getOrFetchItemById(itemId)
        itemService.enrichItem(fetchedItem.entity, fetchedItem.original?.meta)
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
            nftItemControllerApi.getNftItemsByCollection(collection, null, continuation, size)
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

            // Looking for full orders for existing items in order-indexer
            val shortOrderIds = existingItems.values
                .map { listOfNotNull(it.bestBidOrder?.hash, it.bestSellOrder?.hash) }
                .flatten()

            val orders = orderService.getByIds(shortOrderIds).associateBy { it.hash }

            val result = items.map {
                val itemId = ItemId.parseId(it.id)
                val existingItem = existingItems[itemId]
                if (existingItem == null) {
                    // No enrichment data found, item proxied "as is"
                    NftItemDtoToNftOrderItemDtoConverter.convert(it)
                } else {
                    // Enriched item found, using it for response
                    ItemToDtoConverter.convert(existingItem, it.meta!!, orders)
                }
            }

            NftOrderItemsPageDto(itemsResponse.continuation, result)
        }
    }

}
