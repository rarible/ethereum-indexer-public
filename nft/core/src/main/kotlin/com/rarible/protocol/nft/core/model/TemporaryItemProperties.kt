package com.rarible.protocol.nft.core.model

import com.rarible.core.common.nowMillis
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "temporary_item_properties")
data class TemporaryItemProperties(
    val id: String,
    val value: ItemProperties,
    val createDate: Instant = nowMillis()
)
