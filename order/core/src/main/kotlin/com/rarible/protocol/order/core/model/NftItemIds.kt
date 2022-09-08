package com.rarible.protocol.order.core.model

data class NftItemIds(
    val ids: List<String>,
    val continuation: String?
)