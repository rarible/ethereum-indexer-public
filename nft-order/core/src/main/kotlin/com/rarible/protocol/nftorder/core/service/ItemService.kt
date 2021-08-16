package com.rarible.protocol.nftorder.core.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.convert
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.data.ItemEnrichmentData
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.repository.ItemRepository
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
        val result = itemRepository.delete(itemId)
        logger.debug("Deleting Item [{}], deleted: {}", itemId, result?.deletedCount)
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
        logger.debug("Item [{}] not found in DB, fetching from NFT-Indexer", itemId)
        val nftItemDto = nftItemControllerApi
            .getNftItemById(itemId.decimalStringValue, null)
            .awaitFirstOrNull()!!
        logger.debug("Item [{}] fetched, gathering enrichment data", itemId)
        return enrichDto(nftItemDto)
    }

    suspend fun enrichDto(nftItem: NftItemDto): Item {
        val itemId = ItemId(nftItem.contract, EthUInt256(nftItem.tokenId))
        val enrichmentData = getEnrichmentData(itemId)
        val rawItem = conversionService.convert<Item>(nftItem)
        return enrichItem(rawItem, enrichmentData)
    }

    suspend fun enrichItem(rawItem: Item, itemEnrichmentData: ItemEnrichmentData): Item {
        return rawItem.copy(
            totalStock = itemEnrichmentData.totalStock,
            bestSellOrder = itemEnrichmentData.bestSellOrder,
            bestBidOrder = itemEnrichmentData.bestBidOrder,
            unlockable = itemEnrichmentData.unlockable
        )
    }

    suspend fun getEnrichmentData(itemId: ItemId) = coroutineScope {
        val totalStock = async { ownershipService.getOwnershipsTotalStock(itemId) }
        val bestBidOrder = async { orderService.getBestBid(itemId) }
        val bestSellOrder = async { orderService.getBestSell(itemId) }
        val unlockable = async { lockService.isUnlockable(itemId) }

        val result = ItemEnrichmentData(
            totalStock = totalStock.await(),
            bestBidOrder = bestBidOrder.await(),
            bestSellOrder = bestSellOrder.await(),
            unlockable = unlockable.await()
        )
        logger.debug(
            "Enrichment data for Item [{}] fetched:" +
                    " totalStock={}," +
                    " bestBidOrder=[{}]," +
                    " bestSellOrder=[{}]," +
                    " unlockable={}",
            itemId, result.totalStock, result.bestBidOrder?.hash, result.bestSellOrder?.hash, result.unlockable
        )
        result
    }
}