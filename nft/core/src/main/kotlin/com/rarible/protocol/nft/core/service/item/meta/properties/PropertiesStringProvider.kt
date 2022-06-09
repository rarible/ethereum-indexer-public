package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.cache.ContentCache
import com.rarible.protocol.nft.core.service.item.meta.cache.PropertiesStringCacheService
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL

@Component
class PropertiesStringProvider(
    private val propertiesStringCacheService: PropertiesStringCacheService,
    private val urlService: UrlService,
    private val externalHttpClient: ExternalHttpClient,
    private var featureFlags: FeatureFlags
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContent(itemId: ItemId, resource: UrlResource): String? {
        val cache = getCache(resource)
        getFromCache(cache, resource)?.let { return it }

        val fetched = fetch(resource, itemId)
        updateCache(cache, resource, fetched, itemId)

        return fetched
    }

    private fun getCache(resource: UrlResource): ContentCache? {
        return if (featureFlags.enablePropertiesStringCache) {
            propertiesStringCacheService.getCache(resource)
        } else {
            null
        }
    }

    private suspend fun getFromCache(cache: ContentCache?, resource: UrlResource): String? {
        cache ?: return null
        val fromCache = cache.get(resource) ?: return null
        return fromCache.content
    }

    private suspend fun updateCache(cache: ContentCache?, resource: UrlResource, result: String?, itemId: ItemId) {
        cache ?: return
        when {
            result == null -> logger.warn("Can't save content to cache - content not found. $itemId $resource")
            result.isBlank() -> logger.warn("Can't save content to cache - content is empty. $itemId $resource")
            else -> cache.save(resource, result)
        }
    }

    private suspend fun fetch(resource: UrlResource, itemId: ItemId): String? {
        val internalUrl = urlService.resolveInternalHttpUrl(resource)

        if (internalUrl == resource.original) {
            logMetaLoading(itemId, "Fetching content meta by URL $internalUrl")
        } else {
            logMetaLoading(itemId, "Fetching content meta by URL $internalUrl (original URL is ${resource.original})")
        }

        try {
            URL(internalUrl)
        } catch (e: Throwable) {
            logMetaLoading(itemId, "Wrong URL: $internalUrl, $e")
            return null
        }

        return try {
            val propertiesString = externalHttpClient.getBody(url = internalUrl, id = itemId.decimalStringValue) ?: return null

            propertiesString
        } catch (e: Exception) {
            logMetaLoading(itemId, "Failed to receive content meta via URL $internalUrl $e")
            null
        }
    }
}
