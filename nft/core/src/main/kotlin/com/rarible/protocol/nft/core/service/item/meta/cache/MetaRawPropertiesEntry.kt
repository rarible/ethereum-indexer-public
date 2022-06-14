package com.rarible.protocol.nft.core.service.item.meta.cache

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("meta_raw_properties_cache")
data class MetaRawPropertiesEntry(
    @Id
    val url: String,
    val updatedAt: Instant,
    val content: String
)
