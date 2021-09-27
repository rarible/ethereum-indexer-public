package com.rarible.protocol.nftorder.core.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nftorder.core.converter.ItemToDtoConverter
import com.rarible.protocol.nftorder.core.converter.NftItemDtoConverter
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.repository.ItemRepository
import com.rarible.protocol.nftorder.core.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemService(
    private val nftItemControllerApi: NftItemControllerApi,
    private val itemRepository: ItemRepository,
    private val orderService: OrderService
) {

    private val logger = LoggerFactory.getLogger(ItemService::class.java)

    suspend fun get(itemId: ItemId): Item? {
        return itemRepository.get(itemId)
    }

    suspend fun save(item: Item): Item {
        return itemRepository.save(item)
    }

    suspend fun delete(itemId: ItemId): DeleteResult? {
        val now = nowMillis()
        val result = itemRepository.delete(itemId)
        logger.info("Deleting Item [{}], deleted: {} ({}ms)", itemId, result?.deletedCount, spent(now))
        return result
    }

    suspend fun findAll(ids: List<ItemId>): List<Item> {
        return itemRepository.findAll(ids)
    }

    suspend fun getOrFetchItemById(itemId: ItemId): Fetched<Item, NftItemDto> {
        val item = get(itemId)
        return if (item != null) {
            Fetched(item, null)
        } else {
            val now = nowMillis()
            val nftItemDto = nftItemControllerApi
                .getNftItemById(itemId.decimalStringValue)
                .awaitFirstOrNull()!!

            logger.info("Fetched Item by Id [{}] ({}ms)", itemId, spent(now))
            // We can use meta already fetched from indexer to avoid unnecessary getMeta calls later
            val fetchedItem = NftItemDtoConverter.convert(nftItemDto)
            Fetched(fetchedItem, nftItemDto)
        }
    }

    // Here we could specify Order already fetched (or received via event) to avoid unnecessary getById call
    // if one of Item's short orders has same hash
    suspend fun enrichItem(item: Item, meta: NftItemMetaDto?, order: OrderDto? = null) = coroutineScope {
        val fetchedMeta = async { meta ?: fetchItemMetaById(item.id) }
        val bestSellOrder = async { orderService.fetchOrderIfDiffers(item.bestSellOrder, order) }
        val bestBidOrder = async { orderService.fetchOrderIfDiffers(item.bestBidOrder, order) }

        val orders = listOf(bestSellOrder, bestBidOrder)
            .mapNotNull { it.await() }
            .associateBy { it.hash }

        ItemToDtoConverter.convert(item, fetchedMeta.await(), orders)
    }

    suspend fun fetchItemMetaById(itemId: ItemId): NftItemMetaDto {
        return nftItemControllerApi
            .getNftItemMetaById(itemId.decimalStringValue)
            .awaitFirst()
    }
}
