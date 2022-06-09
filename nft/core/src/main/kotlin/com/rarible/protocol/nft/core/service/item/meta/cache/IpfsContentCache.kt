package com.rarible.protocol.nft.core.service.item.meta.cache

import com.rarible.core.common.ifNotBlank
import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.model.UrlResource
import org.springframework.stereotype.Component

@Component
class IpfsContentCache(
    private val storage: ContentCacheStorage<ImmutableMetaCacheEntry>
) : ContentCache {

    override suspend fun get(urlResource: UrlResource): ImmutableMetaCacheEntry? {
        val urlKey = getUrlKey(urlResource)
        return storage.get(urlKey)
    }

    override suspend fun save(urlResource: UrlResource, content: String): ImmutableMetaCacheEntry {
        content.ifNotBlank() ?: throw ContentCacheException("Can't save content to cache - content is empty. $urlResource")

        val entry = ImmutableMetaCacheEntry(
            url = getUrlKey(urlResource),
            updatedAt = nowMillis(),
            content = content
        )

        storage.save(entry)
        return entry
    }

    override fun isSupported(urlResource: UrlResource): Boolean {
        return urlResource is IpfsUrl
    }

    fun getUrlKey(urlResource: UrlResource): String {
        if (!isSupported(urlResource)) {
            throw ContentCacheException("${javaClass.simpleName} doesn't support URL resource $urlResource")
        }
        return (urlResource as IpfsUrl).toSchemaUrl()
    }

}
