package com.rarible.protocol.nft.core.service.item.meta

@Deprecated("This class is only used to migrate cache_meta database table to new meta cache.")
data class CachedContentMeta(
    val type: String,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null
)
