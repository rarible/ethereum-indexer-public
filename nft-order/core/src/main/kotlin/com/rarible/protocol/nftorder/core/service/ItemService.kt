package com.rarible.protocol.nftorder.core.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.convert
import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.repository.ItemRepository
import com.rarible.protocol.nftorder.core.util.spent
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class ItemService(
    private val conversionService: ConversionService,
    private val nftItemControllerApi: NftItemControllerApi,
    private val itemRepository: ItemRepository
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

    suspend fun getOrFetchItemById(itemId: ItemId): Fetched<Item> {
        val item = get(itemId)
        return if (item != null) {
            Fetched(item, false)
        } else {
            Fetched(fetchItem(itemId), true)
        }
    }

    suspend fun fetchItemMetaById(itemId: ItemId): NftItemMetaDto {
        return nftItemControllerApi
            .getNftItemMetaById(itemId.decimalStringValue)
            .awaitFirst()
    }

    private suspend fun fetchItem(itemId: ItemId): Item {
        val now = nowMillis()
        val nftItemDto = nftItemControllerApi
            .getNftItemById(itemId.decimalStringValue)
            .awaitFirstOrNull()!!

        logger.info("Fetched Item by Id [{}] ({}ms)", itemId, spent(now))
        return conversionService.convert(nftItemDto)
    }
}
