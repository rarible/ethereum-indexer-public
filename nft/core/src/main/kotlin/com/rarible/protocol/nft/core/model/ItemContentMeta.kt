package com.rarible.protocol.nft.core.model

import com.rarible.core.content.meta.loader.ContentMeta

data class ItemContentMeta(
    val imageMeta: ContentMeta?,
    val animationMeta: ContentMeta?
)
