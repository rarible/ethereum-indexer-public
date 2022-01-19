package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ITEM_META_CAPTURE_SPAN_TYPE
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class ItemPropertiesService(
    private val itemPropertiesResolverProvider: ItemPropertiesResolverProvider,
    private val ipfsService: IpfsService
) {

    suspend fun resolve(itemId: ItemId): ItemProperties? {
        val resolveResult = doResolve(itemId) ?: return null
        return resolveResult.fixIpfsUrls()
    }

    private suspend fun callResolvers(itemId: ItemId): ItemProperties? {
        for (resolver in itemPropertiesResolverProvider.orderedResolvers) {
            try {
                val itemProperties = resolver.resolve(itemId)
                if (itemProperties != null) {
                    return itemProperties
                }
            } catch (e: Exception) {
                logMetaLoading(itemId, "failed to resolve using ${resolver.name}: ${e.message}", warn = true)
            }
        }
        return null
    }

    private suspend fun doResolve(itemId: ItemId): ItemProperties? {
        logMetaLoading(itemId, "started getting")
        val itemProperties = try {
            callResolvers(itemId)
        } catch (e: Exception) {
            logMetaLoading(itemId, "failed: ${e.message}", warn = true)
            return fallbackToOpenSea(itemId)
        }
        if (itemProperties == null) {
            logMetaLoading(itemId, "not found")
            return fallbackToOpenSea(itemId)
        }
        if (itemProperties.name.isNotBlank()
            && itemProperties.image != null
            && itemProperties.imagePreview != null
            && itemProperties.imageBig != null
            && itemProperties.attributes.isNotEmpty()
        ) {
            logMetaLoading(itemId, "fetched item meta solely with Rarible algorithm")
            return itemProperties
        }
        return extendWithOpenSea(itemId, itemProperties)
    }

    private suspend fun extendWithOpenSea(itemId: ItemId, itemProperties: ItemProperties): ItemProperties {
        logMetaLoading(itemId, "resolving additional properties using OpenSea")
        val openSeaResult = resolveOpenSeaProperties(itemId) ?: return itemProperties
        return extendWithOpenSea(itemProperties, openSeaResult, itemId)
    }

    private suspend fun fallbackToOpenSea(itemId: ItemId): ItemProperties? {
        logMetaLoading(itemId, "falling back to OpenSea")
        return resolveOpenSeaProperties(itemId)
    }

    private suspend fun resolveOpenSeaProperties(itemId: ItemId): ItemProperties? = try {
        itemPropertiesResolverProvider.openSeaResolver.resolve(itemId)
    } catch (e: Exception) {
        logMetaLoading(itemId, "unable to get properties from OpenSea: ${e.message}", warn = true)
        null
    }

    private fun extendWithOpenSea(
        rarible: ItemProperties,
        openSea: ItemProperties,
        itemId: ItemId
    ): ItemProperties {
        fun <T> extend(rarible: T, openSea: T, fieldName: String): T {
            if (openSea != null && rarible != openSea) {
                if (rarible is List<*>
                    && openSea is List<*>
                    && rarible.sortedBy { it.toString() } == openSea.sortedBy { it.toString() }
                ) {
                    return rarible
                }
                if (fieldName == "name"
                    && rarible is String
                    && rarible.isNotEmpty()
                    && openSea is String
                    && openSea.endsWith("#${itemId.tokenId.value}")
                ) {
                    // Apparently, the OpenSea resolver has returned a dummy name as <collection name> #tokenId
                    // We don't want to override the Rarible resolver result.
                    return rarible
                }
                if (fieldName == "image"
                    && rarible is String
                    && rarible.isNotEmpty()
                ) {
                    // Sometimes OpenSea returns invalid original image URL.
                    return rarible
                }
                logMetaLoading(itemId, "extending $fieldName from OpenSea: rarible = [$rarible], openSea = [$openSea]")
                return openSea
            }
            return rarible
        }
        return ItemProperties(
            name = extend(rarible.name, openSea.name, "name"),
            description = extend(rarible.description, openSea.description, "description"),
            image = extend(rarible.image, openSea.image, "image"),
            imageBig = extend(rarible.imageBig, openSea.imageBig, "imageBig"),
            imagePreview = extend(rarible.imagePreview, openSea.imagePreview, "imagePreview"),
            animationUrl = extend(rarible.animationUrl, openSea.animationUrl, "animationUrl"),
            attributes = extend(rarible.attributes, openSea.attributes, fieldName = "attributes"),
            rawJsonContent = extend(rarible.rawJsonContent, openSea.rawJsonContent, "rawJsonContent")
        )
    }

    private fun ItemProperties.fixIpfsUrls(): ItemProperties {
        fun String?.resolveHttpUrl() = if (this != null) ipfsService.resolveHttpUrl(this) else null
        return copy(
            image = image.resolveHttpUrl(),
            imagePreview = imagePreview.resolveHttpUrl(),
            imageBig = imageBig.resolveHttpUrl(),
            animationUrl = animationUrl.resolveHttpUrl()
        )
    }
}
