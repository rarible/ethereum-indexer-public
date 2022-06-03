package com.rarible.protocol.nft.core.model

import java.time.Instant

data class ItemProperties(
    val name: String,
    val description: String?,
    val image: String?,
    val imagePreview: String?,
    val imageBig: String?,
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
)

data class ItemAttribute(
    val key: String,
    val value: String? = null,
    val type: String? = null,
    val format: String? = null
)
