package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaLoader
import com.rarible.protocol.nft.core.service.IpfsService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component

@Component
class MediaMetaService(
    private val ipfsService: IpfsService,
    private val template: ReactiveMongoTemplate,
    private val contentMetaLoader: ContentMetaLoader
) {

    private val logger = LoggerFactory.getLogger(MediaMetaService::class.java)

    suspend fun getMediaMeta(url: String): ContentMeta? {
        val realUrl = ipfsService.resolveHttpUrl(url)
        val fromCache = fetchFromCache(realUrl)
        if (fromCache != null) {
            return fromCache
        }
        val contentMeta = try {
            contentMetaLoader.fetchContentMeta(realUrl)
        } catch (e: Exception) {
            logger.warn("Content meta resolution: error for URL {}", realUrl, e)
            return null
        }
        if (contentMeta == null) {
            logger.warn("Content meta resolution: nothing was resolved by URL {}", realUrl)
        }
        return contentMeta
    }

    private suspend fun fetchFromCache(url: String): ContentMeta? {
        for (candidateUrl in getCandidateUrls(url)) {
            val cacheEntry = template.findById<CachedContentMetaEntry>(
                id = candidateUrl,
                collectionName = CachedContentMetaEntry.CACHE_META_COLLECTION
            ).awaitFirstOrNull()
            if (cacheEntry != null) {
                return cacheEntry.data.let {
                    ContentMeta(
                        type = it.type,
                        width = it.width,
                        height = it.height,
                        size = it.size
                    )
                }
            }
        }
        return null
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getCandidateUrls(url: String): List<String> =
        buildList {
            add(url)
            val hash = getIpfsHash(url)
            if (hash != null) {
                ipfsPrefixes.mapTo(this) { it + hash }
            }
        }.distinct()

    private fun getIpfsHash(url: String): String? {
        for (prefix in ipfsPrefixes) {
            if (url.startsWith(prefix)) {
                return url.substringAfter(prefix)
            }
        }
        return null
    }

    companion object {
        private const val rariblePinata = "https://rarible.mypinata.cloud/ipfs/"
        private const val ipfsRarible = "https://ipfs.rarible.com/ipfs/"
        private val ipfsPrefixes = listOf(rariblePinata, ipfsRarible)
    }
}
