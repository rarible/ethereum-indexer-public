package com.rarible.protocol.nft.core.service.item.meta

import org.springframework.data.annotation.Id

@Deprecated("This class is only used to migrate cache_meta database table to new meta cache.")
data class CachedContentMetaEntry(
    @Id
    val id: String,
    val data: CachedContentMeta
) {
    companion object {
        const val CACHE_META_COLLECTION = "cache_meta"
    }
}
