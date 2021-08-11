package com.rarible.protocol.nft.core.model

data class ContentMeta(
    val imageMeta: MediaMeta?,
    val animationMeta: MediaMeta?
)

data class MediaMeta(
    val type: String,
    val width: Int? = null,
    val height: Int? = null
)
