package com.rarible.protocol.nftorder.listener.service

import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.data.ItemEnrichmentData
import com.rarible.protocol.nftorder.core.data.OwnershipEnrichmentData
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.core.service.OrderService
import com.rarible.protocol.nftorder.core.service.OwnershipService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderEventService(
    private val orderService: OrderService,
    private val bestOrderService: BestOrderService,
    private val itemService: ItemService,
    private val ownershipService: OwnershipService,
    private val itemEventService: ItemEventService,
    private val ownershipEventService: OwnershipEventService
) {

    private val logger = LoggerFactory.getLogger(OrderEventService::class.java)

    suspend fun updateOrder(order: OrderDto) = coroutineScope {

        val makeItemId = toItemId(order.make.assetType)
        val takeItemId = toItemId(order.take.assetType)

        // Reading or fetching data required update, if make/take item ids are present
        val ownershipFuture = makeItemId?.let {
            async {
                val ownershipId = OwnershipId(makeItemId.token, makeItemId.tokenId, order.maker)
                ownershipService.getOrFetchOwnershipById(ownershipId)
            }
        }

        val makeItemFuture = makeItemId?.let { async { itemService.getOrFetchItemById(makeItemId) } }
        val takeItemFuture = takeItemId?.let { async { itemService.getOrFetchItemById(takeItemId) } }

        val fetchedOwnership = ownershipFuture?.await()
        val makeItem = makeItemFuture?.await()
        val takeItem = takeItemFuture?.await()

        // Fetching actual best bid/sell data from adjusted services via HTTP API
        val bestOwnershipSellOrder =
            fetchedOwnership?.let { async { fetchBestSellOrderForOwnership(fetchedOwnership, order) } }
        val bestSellOrder = makeItem?.let { async { fetchBestSellOrderForItem(makeItem, order) } }
        val bestBidOrder = takeItem?.let { async { fetchBestBidOrderForItem(takeItem, order) } }

        // Updating entities in local DB via event-services in order to emit related events
        updateMakeItem(makeItem?.entity, bestSellOrder?.await())
        updateTakeItem(takeItem?.entity, bestBidOrder?.await())
        updateOwnership(fetchedOwnership?.entity, bestOwnershipSellOrder?.await())
    }

    // If Ownership or Item just fetched, it means, it is already have actual enriched data,
    // we don't need to retrieve it again.
    // But it also means such records are not stored in DB, and we have to store them via regular update
    private suspend fun fetchBestSellOrderForOwnership(
        fetchedOwnership: Fetched<Ownership>,
        order: OrderDto
    ): OrderDto? {
        val ownership = fetchedOwnership.entity
        return if (fetchedOwnership.isFetched) {
            logger.info(
                "Ownership [{}] was fetched, using fetched BestSellOrder: [{}]",
                ownership.id, ownership.bestSellOrder
            )
            ownership.bestSellOrder
        } else {
            bestOrderService.getBestSellOrder(ownership, order)
        }
    }

    private suspend fun fetchBestSellOrderForItem(
        fetchedItem: Fetched<Item>,
        order: OrderDto
    ): OrderDto? {
        val item = fetchedItem.entity
        return if (fetchedItem.isFetched) {
            logger.info("Item [{}] was fetched, using fetched BestSellOrder: [{}]", item.id, item.bestSellOrder)
            item.bestSellOrder
        } else {
            bestOrderService.getBestSellOrder(item, order)
        }
    }

    private suspend fun fetchBestBidOrderForItem(
        fetchedItem: Fetched<Item>,
        order: OrderDto
    ): OrderDto? {
        val item = fetchedItem.entity
        return if (fetchedItem.isFetched) {
            logger.info("Item [{}] was fetched, using fetched BestBidOrder: [{}]", item.id, item.bestBidOrder)
            item.bestBidOrder
        } else {
            bestOrderService.getBestBidOrder(item, order)
        }
    }

    private suspend fun updateOwnership(ownership: Ownership?, bestOwnershipSellOrder: OrderDto?) {
        ownership?.let {
            // TODO - maybe do not update if nothing changed?
            logger.info(
                "Updating bestSell Order for Ownership [{}]: [{}] -> [{}]",
                ownership.id, ownership.bestSellOrder?.hash, bestOwnershipSellOrder?.hash
            )

            val enrichmentData = OwnershipEnrichmentData(bestOwnershipSellOrder)

            ownershipEventService.onOwnershipUpdated(ownership, enrichmentData)
        }
    }

    private suspend fun updateMakeItem(makeItem: Item?, bestSellOrder: OrderDto?) {
        makeItem?.let {
            // TODO - maybe do not update if nothing changed?
            logger.info(
                "Updating bestSell Order for make-Item [{}]: [{}] -> [{}]",
                makeItem.id, makeItem.bestSellOrder?.hash, bestSellOrder?.hash
            )

            val enrichmentData = ItemEnrichmentData(
                sellers = makeItem.sellers,
                totalStock = makeItem.totalStock,
                bestSellOrder = bestSellOrder,
                bestBidOrder = makeItem.bestBidOrder,
                unlockable = makeItem.unlockable
            )
            itemEventService.onItemUpdated(makeItem, enrichmentData)
        }
    }

    private suspend fun updateTakeItem(takeItem: Item?, bestBidOrder: OrderDto?) {
        takeItem?.let {
            // TODO - maybe do not update if nothing changed?
            logger.info(
                "Updating bestBid Order for take-Item [{}]: [{}] -> [{}]",
                takeItem.id, takeItem.bestBidOrder?.hash, bestBidOrder?.hash
            )

            val enrichmentData = ItemEnrichmentData(
                sellers = takeItem.sellers,
                totalStock = takeItem.totalStock,
                bestSellOrder = takeItem.bestSellOrder,
                bestBidOrder = bestBidOrder,
                unlockable = takeItem.unlockable
            )
            itemEventService.onItemUpdated(takeItem, enrichmentData)
        }
    }

    private fun toItemId(assetType: AssetTypeDto): ItemId? {

        return when (assetType) {
            is Erc721AssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId)
            is Erc1155AssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId)
            is Erc721LazyAssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId)
            is Erc1155LazyAssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId)
            is EthAssetTypeDto -> null
            is FlowAssetTypeDto -> null
            is Erc20AssetTypeDto -> null
        }
    }

}