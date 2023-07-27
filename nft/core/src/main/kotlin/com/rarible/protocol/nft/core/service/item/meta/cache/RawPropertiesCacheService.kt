package com.rarible.protocol.nft.core.service.item.meta.cache

import com.rarible.core.meta.resource.model.UrlResource
import org.springframework.stereotype.Component

@Component
class RawPropertiesCacheService(
    private val caches: List<ContentCache>
) {

    fun getCache(urlResource: UrlResource): ContentCache? {
        return caches.find { it.isSupported(urlResource) }
    }
}
