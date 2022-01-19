package com.rarible.protocol.nft.core.data

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemContentMeta
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties

fun randomItemMeta() = ItemMeta(
    properties = randomItemProperties(),
    itemContentMeta = randomContentMeta()
)

fun randomContentMeta() = ItemContentMeta(
    imageMeta = randomMediaMeta(),
    animationMeta = randomMediaMeta()
)

fun randomMediaMeta() = ContentMeta(
    type = randomString(),
    width = randomInt(),
    height = randomInt()
)

fun randomItemProperties() = ItemProperties(
    name = randomString(),
    description = randomString(),
    image = randomString(),
    imagePreview = randomString(),
    imageBig = randomString(),
    animationUrl = randomString(),
    attributes = listOf(ItemAttribute(randomString(), randomString(), randomString(), randomString())),
    rawJsonContent = """{"name": "value"}"""
)
