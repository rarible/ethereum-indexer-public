package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.opensea.client.model.v1.Asset
import com.rarible.opensea.client.model.v1.OpenSeaAssets
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemExState
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.SuspiciousUpdateResult
import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
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
    private val listenerMetrics: NftListenerMetricsFactory
) {
    suspend fun update(asset: UpdateSuspiciousItemsState.Asset): SuspiciousUpdateResult {
        return try {
            logger.info("Get items to check suspicious: ${asset.contract}, cursor${asset.cursor}")
            val openSeaAssets = getOpenSeaAssets(asset)
            coroutineScope {
                openSeaAssets.assets
                    .map { item -> async {
                        val itemId = ItemId(item.assetContract.address, item.tokenId)
                        val suspicious = getSuspicious(item)
                        updateItem(itemId, suspicious)
                    } }
                    .awaitAll()
            }
            SuspiciousUpdateResult.Success(asset.copy(cursor = openSeaAssets.next))
        } catch (ex: Throwable) {
            logger.error("Can't get OpenSea assets: ${asset.contract}, cursor=${asset.cursor}", ex)
            SuspiciousUpdateResult.Fail(asset)
        }
    }

    private suspend fun updateItem(itemId: ItemId, suspicious: Boolean) {
        val item = itemRepository.findById(itemId).awaitFirstOrNull() ?: run {
            logger.error("Can't find item {} to update suspicious", itemId.decimalStringValue)
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
        listenerMetrics.onSuspiciousItemUpdate()
    }

    private suspend fun updateState(item: Item) {
        val itemId = item.id
        val exState = itemStateRepository.getById(itemId) ?: ItemExState.initial(itemId)
        val updatedState = exState.withSuspiciousOnOS(item.isSuspiciousOnOS)
        itemStateRepository.save(updatedState)
    }

    private suspend fun getOpenSeaAssets(asset: UpdateSuspiciousItemsState.Asset): OpenSeaAssets {
        return openSeaService.getOpenSeaAssets(asset.contract, asset.cursor).also {
            listenerMetrics.onSuspiciousItemsGet(it.assets.size)
        }
    }

    private fun getSuspicious(asset: Asset): Boolean {
        val suspicious = asset.supportsWyvern.not()
        if (suspicious) {
            logger.info("Found suspicious: ${asset.assetContract.address}:${asset.tokenId}")
            listenerMetrics.onSuspiciousItemFound()
        }
        return suspicious
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SuspiciousItemsService::class.java)
    }
}
