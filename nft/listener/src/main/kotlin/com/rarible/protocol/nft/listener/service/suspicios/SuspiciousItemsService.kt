package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemExState
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import com.rarible.protocol.nft.listener.service.opensea.OpenSeaService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SuspiciousItemsService(
    private val itemRepository: ItemRepository,
    private val itemStateRepository: ItemExStateRepository,
    private val openSeaService: OpenSeaService,
    private val itemUpdateService: ItemUpdateService,
) {
    suspend fun update(asset: UpdateSuspiciousItemsState.Asset): UpdateSuspiciousItemsState.Asset {
        val openSeaAssets = openSeaService.getOpenSeaAssets(asset.contract, asset.cursor)

        coroutineScope {
            openSeaAssets.assets
                .map { item -> async {
                    val itemId = ItemId(item.assetContract.address, item.tokenId)
                    val suspicious = item.supportsWyvern
                    updateItem(itemId, suspicious)
                } }
                .awaitAll()
        }
        return asset.copy(cursor = openSeaAssets.next)
    }

    private suspend fun updateItem(itemId: ItemId, suspicious: Boolean) {
        val item = itemRepository.findById(itemId).awaitFirstOrNull() ?: run {
            logger.error("Can't fin item {} to update suspicious", itemId.decimalStringValue)
            return
        }
        if (item.isSuspiciousOnOS != suspicious) {
            logger.info("Update item suspicious: {}, {} -> {}",
                itemId.decimalStringValue, item.isSuspiciousOnOS, suspicious
            )
            item.withSuspiciousOnOS(suspicious).apply {
                updateState(this)
                updateItem(this)
            }
        }
    }

    private suspend fun updateItem(item: Item) {
        itemUpdateService.update(item)
    }

    private suspend fun updateState(item: Item) {
        val itemId = item.id
        val exState = itemStateRepository.getById(itemId) ?: ItemExState.initial(itemId)
        val updatedState = exState.withSuspiciousOnOS(item.isSuspiciousOnOS)
        itemStateRepository.save(updatedState)
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SuspiciousItemsService::class.java)
    }
}