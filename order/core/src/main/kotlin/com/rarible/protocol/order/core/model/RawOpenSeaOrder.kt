package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("order")
data class RawOpenSeaOrder(
    @Id
    val id: String,
    val value: String
)
