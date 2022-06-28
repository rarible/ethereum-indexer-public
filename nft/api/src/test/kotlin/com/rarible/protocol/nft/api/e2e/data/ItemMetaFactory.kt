package com.rarible.protocol.nft.api.e2e.data

import com.rarible.core.test.data.randomString
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder

fun randomItemMeta() = ItemMeta(
    properties = randomItemProperties()
)

fun randomItemProperties() = ItemProperties(
    name = randomString(),
    description = randomString(),
    attributes = listOf(ItemAttribute(randomString(), randomString(), randomString(), randomString())),
    rawJsonContent = """{"name": "value"}""",
    content = ContentBuilder.getItemMetaContent(
        imageOriginal = randomString(),
        imageBig = randomString(),
        imagePreview = randomString(),
        videoOriginal = randomString()
    )
)
