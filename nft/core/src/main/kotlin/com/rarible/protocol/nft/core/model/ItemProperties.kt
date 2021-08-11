package com.rarible.protocol.nft.core.model

data class ItemProperties(
    val name: String,
    val description: String?,
    val image: String?,
    val imagePreview: String?,
    val imageBig: String?,
    val animationUrl: String? = null,
    val attributes: List<ItemAttribute>
)

data class ItemAttribute(
    val key: String,
    val value: String?
)