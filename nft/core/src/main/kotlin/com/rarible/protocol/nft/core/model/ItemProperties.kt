package com.rarible.protocol.nft.core.model

data class ItemProperties(
    val name: String,
    val description: String?,
    val image: String?,
    val imagePreview: String?,
    val imageBig: String?,
    val animationUrl: String? = null,
    val attributes: List<ItemAttribute>
) {
    companion object {
        val EMPTY = ItemProperties(
            name = "Untitled",
            description = null,
            image = null,
            imagePreview = null,
            imageBig = null,
            attributes = listOf()
        )
    }
}

data class ItemAttribute(
    val key: String,
    val value: String? = null,
    val type: String? = null,
    val format: String? = null
)
