package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.cache.ContentCache
import com.rarible.protocol.nft.core.service.item.meta.cache.RawPropertiesCacheService
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL

@Component
class RawPropertiesProvider(
    private val rawPropertiesCacheService: RawPropertiesCacheService,
    private val urlService: UrlService,
    private val externalHttpClient: ExternalHttpClient,
    private var featureFlags: FeatureFlags
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContent(itemId: ItemId, resource: UrlResource): String? {
        val cache = getCache(resource)
        getFromCache(cache, resource)?.let { return it }

        // We want to use proxy only for regular HTTP urls, IPFS URLs should not be proxied
        // because we want to use our own IPFS nodes in nearest future
        val useProxy = (resource is HttpUrl) && featureFlags.enableProxyForMetaDownload

        val fetched = fetch(resource, itemId, useProxy)
        updateCache(cache, resource, fetched, itemId)

        return fetched
    }

    private fun getCache(resource: UrlResource): ContentCache? {
        return if (featureFlags.enableMetaRawPropertiesCache) {
            rawPropertiesCacheService.getCache(resource)
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
        if (result.isNullOrBlank() || cache == null) {
            return
        }
        cache.save(resource, result)
    }

    private suspend fun fetch(resource: UrlResource, itemId: ItemId, useProxy: Boolean = false): String? {
        val internalUrl = urlService.resolveInternalHttpUrl(resource)

        if (internalUrl == resource.original) {
            logMetaLoading(itemId, "Fetching property string by URL $internalUrl")
        } else {
            logMetaLoading(
                itemId, "Fetching property string by URL $internalUrl (original URL is ${resource.original})"
            )
        }

        try {
            URL(internalUrl)
        } catch (e: Throwable) {
            logMetaLoading(itemId, "Wrong URL: $internalUrl, $e")
            return null
        }

        return try {
            externalHttpClient.getBody(
                url = internalUrl,
                id = itemId.toString(),
                useProxy = useProxy
            )
        } catch (e: Exception) {
            logMetaLoading(itemId, "Failed to receive property string via URL $internalUrl $e")
            null
        }
    }
}
