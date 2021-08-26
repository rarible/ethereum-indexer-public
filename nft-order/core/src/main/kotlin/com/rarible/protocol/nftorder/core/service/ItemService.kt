package com.rarible.protocol.nftorder.core.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.convert
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.data.ItemEnrichmentData
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.repository.ItemRepository
import com.rarible.protocol.nftorder.core.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class ItemService(
    private val ownershipService: OwnershipService,
    private val orderService: OrderService,
    private val lockService: LockService,
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

    suspend fun getOrFetchEnrichedItemById(itemId: ItemId): Fetched<Item> {
        val item = get(itemId)
        return if (item != null) {
            Fetched(item, false)
        } else {
            Fetched(fetchEnrichedItem(itemId), true)
        }
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

    private suspend fun fetchEnrichedItem(itemId: ItemId): Item {
        val nftItemDto = fetchItemDto(itemId)
        logger.debug("Item [{}] fetched, gathering enrichment data", itemId)
        return enrichDto(nftItemDto)
    }

    private suspend fun fetchItem(itemId: ItemId): Item {
        val nftItemDto = fetchItemDto(itemId)
        logger.debug("Item [{}] fetched", itemId)
        return conversionService.convert<Item>(nftItemDto)
    }

    private suspend fun fetchItemDto(itemId: ItemId): NftItemDto {
        val now = nowMillis()
        val result = nftItemControllerApi
            .getNftItemById(itemId.decimalStringValue, null)
            .awaitFirstOrNull()!!

        logger.info("Fetched Ownership by Id [{}] ({}ms)", itemId, spent(now))
        return result
    }

    suspend fun enrichDto(nftItem: NftItemDto): Item {
        val itemId = ItemId(nftItem.contract, EthUInt256(nftItem.tokenId))
        val enrichmentData = getEnrichmentData(itemId)
        val rawItem = conversionService.convert<Item>(nftItem)
        return enrichItem(rawItem, enrichmentData)
    }

    suspend fun enrichItem(rawItem: Item, itemEnrichmentData: ItemEnrichmentData): Item {
        return rawItem.copy(
            sellers = itemEnrichmentData.sellers,
            totalStock = itemEnrichmentData.totalStock,
            bestSellOrder = itemEnrichmentData.bestSellOrder,
            bestBidOrder = itemEnrichmentData.bestBidOrder,
            unlockable = itemEnrichmentData.unlockable
        )
    }

    suspend fun getEnrichmentData(itemId: ItemId) = coroutineScope {
        val now = nowMillis()
        val sellStatsFuture = async { ownershipService.getItemSellStats(itemId) }
        val bestBidOrderFuture = async { orderService.getBestBid(itemId) }
        val bestSellOrderFuture = async { orderService.getBestSell(itemId) }
        val unlockable = async { lockService.isUnlockable(itemId) }

        val sellStats = sellStatsFuture.await()

        val result = ItemEnrichmentData(
            sellers = sellStats.sellers,
            totalStock = sellStats.totalStock,
            bestBidOrder = bestBidOrderFuture.await(),
            bestSellOrder = bestSellOrderFuture.await(),
            unlockable = unlockable.await()
        )
        logger.info("Enrichment data for Item [{}] fetched: [{}] ({}ms)", itemId, result, spent(now))
        result
    }
}