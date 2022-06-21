package com.rarible.protocol.nft.core.model

@Deprecated("Use EthMetaContent instead")
data class ItemContentMeta( // TODO Remove
    val imageMeta: ContentMeta?,
    val animationMeta: ContentMeta?
)
