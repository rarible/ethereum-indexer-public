package com.rarible.protocol.nft.core.service.item.meta.cache

import com.rarible.core.meta.resource.model.UrlResource

interface ContentCache {

    fun isSupported(urlResource: UrlResource): Boolean

    suspend fun get(urlResource: UrlResource): ImmutableMetaCacheEntry?

    suspend fun save(urlResource: UrlResource, content: String): ImmutableMetaCacheEntry
}
