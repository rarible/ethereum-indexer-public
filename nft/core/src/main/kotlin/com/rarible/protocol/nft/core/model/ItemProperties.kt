package com.rarible.protocol.nft.core.model

import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import java.time.Instant

data class ItemProperties(
    val name: String,
    val description: String?,
    val attributes: List<ItemAttribute>,
    val rawJsonContent: String?,
    val createdAt: Instant? = null,
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val language: String? = null,
    val rights: String? = null,
    val rightsUri: String? = null,
    val externalUri: String? = null,
    val tokenUri: String? = null,
    val content: ItemMetaContent = ItemMetaContent()
)

data class ItemMetaContent(
    val imageOriginal: EthMetaContent? = null,
    val imageBig: EthMetaContent? = null,
    val imagePreview: EthMetaContent? = null,
    val videoOriginal: EthMetaContent? = null,
) {
    fun asList(): List<EthMetaContent> {
        val content = ArrayList<EthMetaContent>(4)

        this.imageOriginal?.let { content.add(it) }
        this.imageBig?.let { content.add(it) }
        this.imagePreview?.let { content.add(it) }
        this.videoOriginal?.let { content.add(it) }

        return content
    }
}

data class ItemAttribute(
    val key: String,
    val value: String? = null,
    val type: String? = null,
    val format: String? = null
)
