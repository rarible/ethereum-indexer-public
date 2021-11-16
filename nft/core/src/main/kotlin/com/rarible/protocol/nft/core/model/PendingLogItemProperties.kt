package com.rarible.protocol.nft.core.model

import com.rarible.core.common.nowMillis
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "pending_log_item_properties")
data class PendingLogItemProperties(
    val id: String,
    val value: ItemProperties,
    val createDate: Instant = nowMillis()
)
