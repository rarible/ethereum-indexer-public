package com.rarible.protocol.nft.core.model

data class ItemMeta(
    val properties: ItemProperties,
    val meta: ContentMeta
) {
    companion object {
        val EMPTY = ItemMeta(ItemProperties.EMPTY, ContentMeta.EMPTY)
    }
}
