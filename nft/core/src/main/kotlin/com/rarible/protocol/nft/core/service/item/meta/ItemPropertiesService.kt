package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import kotlinx.coroutines.TimeoutCancellationException
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class ItemPropertiesService(
    private val itemPropertiesResolverProvider: ItemPropertiesResolverProvider,
    private val openseaItemPropertiesService: OpenseaItemPropertiesService
) {

    private suspend fun callResolvers(itemId: ItemId): ItemProperties? {
        for (resolver in itemPropertiesResolverProvider.orderedResolvers) {
            try {
                val itemProperties = resolver.resolve(itemId)
                if (itemProperties != null) {
                    return itemProperties
                }
            } catch (e: ItemResolutionAbortedException) {
                throw e // re-throw upper
            } catch (e: TimeoutCancellationException) {
                logMetaLoading(itemId, "failed to resolve using ${resolver.name}: ${e.message}", warn = true)
                return null // Meta resolution timed out, return null
            } catch (e: Exception) {
                logMetaLoading(itemId, "failed to resolve using ${resolver.name}: ${e.message}", warn = true)
            }
        }
        return null
    }

    suspend fun resolve(itemId: ItemId): ItemProperties? {
        logMetaLoading(itemId, "started getting")
        val itemProperties = try {
            callResolvers(itemId)
        } catch (e: MetaException) {
            logMetaLoading(itemId, "failed: ${e.message}", warn = true)
            val openSeaItemProperties = openseaItemPropertiesService.fallbackToOpenSea(itemId)

            if (openSeaItemProperties != null) {
                return openSeaItemProperties
            } else {
                throw e
            }
        } catch (e: ItemResolutionAbortedException) {
            logMetaLoading(itemId, "resolution aborted")
            return null
        } catch (e: Exception) {
            logMetaLoading(itemId, "failed: ${e.message}", warn = true)
            return openseaItemPropertiesService.fallbackToOpenSea(itemId)
        }
        if (itemProperties == null) {
            logMetaLoading(itemId, "not found")
            return openseaItemPropertiesService.fallbackToOpenSea(itemId)
        }

        if (itemProperties.isFull()) {
            logMetaLoading(itemId, "fetched item meta solely with Rarible algorithm")
            return itemProperties
        }

        return openseaItemPropertiesService.extendWithOpenSea(itemId, itemProperties)
    }

    private fun ItemProperties.isFull(): Boolean =
        this.name.isNotBlank() &&
            this.content.imageOriginal != null
}
