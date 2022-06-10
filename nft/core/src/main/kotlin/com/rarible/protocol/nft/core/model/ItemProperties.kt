package com.rarible.protocol.nft.core.model

import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import java.time.Instant

data class ItemProperties(
    val name: String,
    val description: String?,
    @Deprecated("Should be replaced by content")
    val image: String?,
    @Deprecated("Should be replaced by content")
    val imagePreview: String?,
    @Deprecated("Should be replaced by content")
    val imageBig: String?,
    @Deprecated("Should be replaced by content")
    val animationUrl: String?,
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
    val content: List<EthMetaContent> = emptyList()
)

data class ItemAttribute(
    val key: String,
    val value: String? = null,
    val type: String? = null,
    val format: String? = null
)
