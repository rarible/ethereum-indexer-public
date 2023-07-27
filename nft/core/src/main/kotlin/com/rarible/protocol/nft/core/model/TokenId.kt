package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("tokenId")
data class TokenId(
    @Id
    val id: String,
    val value: Long
)
