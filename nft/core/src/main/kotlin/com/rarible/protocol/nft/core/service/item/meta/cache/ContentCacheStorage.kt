package com.rarible.protocol.nft.core.service.item.meta.cache

interface ContentCacheStorage<T> {

    suspend fun get(url: String): T?

    suspend fun save(content: T)

    suspend fun delete(url: String)
}
