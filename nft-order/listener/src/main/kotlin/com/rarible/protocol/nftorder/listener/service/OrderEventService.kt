package com.rarible.protocol.nftorder.listener.service

import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.data.ItemEnrichmentData
import com.rarible.protocol.nftorder.core.data.OwnershipEnrichmentData
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.core.service.OrderService
import com.rarible.protocol.nftorder.core.service.OwnershipService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderEventService(
    private val orderService: OrderService,
    private val itemService: ItemService,
    private val ownershipService: OwnershipService,
    private val itemEventService: ItemEventService,
    private val ownershipEventService: OwnershipEventService
) {

    private val logger = LoggerFactory.getLogger(OrderEventService::class.java)

    suspend fun updateOrder(order: OrderDto) {
        val makeItemId = toItemId(order.make.assetType)
        val takeItemId = toItemId(order.take.assetType)

        // Reading or fetching data required update, if make/take item ids are present
        val ownership = makeItemId?.let {
            val ownershipId = OwnershipId(makeItemId.token, makeItemId.tokenId, order.maker)
            ownershipService.getOrFetchOwnershipById(ownershipId)
        }

        val makeItem = makeItemId?.let { itemService.getOrFetchItemById(makeItemId) }
        val takeItem = takeItemId?.let { itemService.getOrFetchItemById(takeItemId) }

        // Fetching actual best bid/sell data from adjusted services via HTTP API
        val bestOwnershipSellOrder = ownership?.let { orderService.getBestSell(ownership.id) }
        val bestSellOrder = makeItemId?.let { orderService.getBestSell(makeItemId) }
        val bestBidOrder = takeItemId?.let { orderService.getBestBid(takeItemId) }

        // Updating entities in local DB via event-services in order to emit related events
        updateOwnership(ownership, bestOwnershipSellOrder)
        updateMakeItem(makeItem, bestSellOrder)
        updateTakeItem(takeItem, bestBidOrder)
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