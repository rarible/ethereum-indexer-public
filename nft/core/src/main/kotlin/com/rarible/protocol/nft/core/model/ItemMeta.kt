package com.rarible.protocol.nft.core.model

import com.rarible.protocol.nft.core.model.meta.EthMetaContent

data class ItemMeta(
    val properties: ItemProperties,
    val itemContentMeta: ItemContentMeta,
    val content: List<EthMetaContent> = emptyList()
)
