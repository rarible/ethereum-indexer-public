package com.rarible.protocol.nft.core.service.item.meta

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("pending_item_token_uri")
data class PendingItemTokenUri(
    @Id
    val itemId: String,
    val tokenUri: String
)