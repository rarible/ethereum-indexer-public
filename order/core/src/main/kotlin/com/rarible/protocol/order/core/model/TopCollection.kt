package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address

@Document("top_collection")
data class TopCollection(
    @Id
    val id: Address
)
