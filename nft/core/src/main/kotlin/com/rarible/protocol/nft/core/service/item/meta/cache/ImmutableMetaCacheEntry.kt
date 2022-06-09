package com.rarible.protocol.nft.core.service.item.meta.cache

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("immutable_meta_content_url")
data class ImmutableMetaCacheEntry(
    @Id
    val url: String,
    val updatedAt: Instant,
    val content: String
)
