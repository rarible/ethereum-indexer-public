package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ITEM_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaLegacyCachePropertiesResolver
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant

@Service
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class ItemPropertiesService(
    private val itemPropertiesResolverProvider: ItemPropertiesResolverProvider,
    private val ipfsService: IpfsService,
    @Value("\${api.properties.cache-timeout}") private val cacheTimeout: Long,
    @Autowired(required = false) private val cacheService: CacheService?
) {

    private data class CachedItemProperties(
        val properties: ItemProperties,
        val fetchAt: Instant,
        val canBeCached: Boolean
    )

    private val cacheDescriptor = object : CacheDescriptor<CachedItemProperties> {
        override val collection get() = ITEM_METADATA_COLLECTION

        override fun get(id: String) = mono {
            val resolveResult = doResolve(ItemId.parseId(id)) ?: return@mono null
            val itemProperties = resolveResult.first.fixIpfsUrls()
            val resolver = resolveResult.second
            CachedItemProperties(itemProperties, nowMillis(), resolver.canBeCached)
        }

        override fun getMaxAge(value: CachedItemProperties?): Long =
            when {
                value == null -> DateUtils.MILLIS_PER_DAY
                value.canBeCached -> cacheTimeout
                else -> 0
            }
    }

    suspend fun resolve(itemId: ItemId): ItemProperties? =
        cacheService.get(
            itemId.decimalStringValue,
            cacheDescriptor,
            immediatelyIfCached = false
        ).awaitFirstOrNull()?.properties

    suspend fun resetProperties(itemId: ItemId) {
        logProperties(itemId, "resetting properties")
        cacheService?.reset(itemId.decimalStringValue, cacheDescriptor)
            ?.onErrorResume { Mono.empty() }
            ?.awaitFirstOrNull()

        itemPropertiesResolverProvider.orderedResolvers.forEach { runCatching { it.reset(itemId) } }
    }

    private suspend fun callResolvers(itemId: ItemId): Pair<ItemProperties, ItemPropertiesResolver>? {
        for (resolver in itemPropertiesResolverProvider.orderedResolvers) {
            try {
                val itemProperties = resolver.resolve(itemId)
                if (itemProperties != null) {
                    return itemProperties to resolver
                }
            } catch (e: Exception) {
                logProperties(itemId, "failed to resolve using ${resolver.name}: ${e.message}", warn = true)
            }
        }
        return null
    }

    private suspend fun doResolve(itemId: ItemId): Pair<ItemProperties, ItemPropertiesResolver>? {
        logProperties(itemId, "started getting")
        val resolveResult = try {
            callResolvers(itemId)
        } catch (e: Exception) {
            logProperties(itemId, "failed: ${e.message}", warn = true)
            return fallbackToOpenSea(itemId)
        }
        if (resolveResult == null) {
            logProperties(itemId, "not found")
            return fallbackToOpenSea(itemId)
        }
        if (resolveResult.second.name == OpenSeaLegacyCachePropertiesResolver.NAME) {
            logProperties(itemId, "returned from legacy OpenSea cache")
            return resolveResult
        }
        val itemProperties = resolveResult.first
        if (itemProperties.name.isNotBlank()
            && itemProperties.image != null
            && itemProperties.imagePreview != null
            && itemProperties.imageBig != null
            && itemProperties.attributes.isNotEmpty()
        ) {
            logProperties(itemId, "fetched item meta solely with Rarible algorithm")
            return resolveResult
        }
        val extendedProperties = extendWithOpenSea(itemId, itemProperties)
        return resolveResult.copy(first = extendedProperties)
    }

    private suspend fun extendWithOpenSea(itemId: ItemId, itemProperties: ItemProperties): ItemProperties {
        logProperties(itemId, "resolving additional properties using OpenSea")
        val openSeaResult = resolveOpenSeaProperties(itemId)?.first ?: return itemProperties
        return extendWithOpenSea(itemProperties, openSeaResult, itemId)
    }

    private suspend fun fallbackToOpenSea(itemId: ItemId): Pair<ItemProperties, ItemPropertiesResolver>? {
        logProperties(itemId, "falling back to OpenSea")
        return resolveOpenSeaProperties(itemId)
    }

    private suspend fun resolveOpenSeaProperties(itemId: ItemId): Pair<ItemProperties, ItemPropertiesResolver>? = try {
        itemPropertiesResolverProvider.openSeaResolver.resolve(itemId)?.let { it to itemPropertiesResolverProvider.openSeaResolver }
    } catch (e: Exception) {
        logProperties(itemId, "unable to get properties from OpenSea: ${e.message}", warn = true)
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
                logProperties(itemId, "extending $fieldName from OpenSea: rarible = [$rarible], openSea = [$openSea]")
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ItemPropertiesService::class.java)

        fun logProperties(itemId: ItemId, message: String, warn: Boolean = false) {
            val logMessage = "Meta of ${itemId.decimalStringValue}: $message"
            if (warn) {
                logger.warn(logMessage)
            } else {
                logger.info(logMessage)
            }
        }

        const val ITEM_METADATA_COLLECTION = "item_metadata"
    }
}
