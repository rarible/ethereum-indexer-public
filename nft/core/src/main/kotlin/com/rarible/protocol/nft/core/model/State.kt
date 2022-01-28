package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("state")
data class State(
    val state: String,
    @Id
    val id: String
)