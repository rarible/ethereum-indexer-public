package com.rarible.protocol.nft.core.service.item.meta.properties

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.parseAttributes

/**
 * Default mapper from JSON to ItemProperties. Can be used for most of the items.
 */
object JsonPropertiesMapper {

    private val FIELD_NAME = listOf(
        "name",
        "label",
        "title"
    ).toTypedArray()

    private val FIELD_DESCRIPTION = listOf(
        "description"
    ).toTypedArray()

    private val FIELD_IMAGE_ORIGINAL = listOf(
        "image",
        "image_url",
        "image_content",
        "image_data",
        "imageUrl"
    ).toTypedArray()

    private val FIELD_IMAGE_PREVIEW = listOf(
        "imagePreview",
        "image_preview",
        "image_preview_url"
    ).toTypedArray()
    private val FIELD_IMAGE_BIG = listOf(
        "imageBig",
        "image_big",
        "image_big_url"
    ).toTypedArray()

    private val FIELD_VIDEO_ORIGINAL = listOf(
        "animation",
        "animation_url",
        "animationUrl"
    ).toTypedArray()

    fun map(itemId: ItemId, node: ObjectNode): ItemProperties {
        return ItemProperties(
            name = node.getText(*FIELD_NAME) ?: "",
            description = node.getText(*FIELD_DESCRIPTION),
            attributes = node.parseAttributes(),
            rawJsonContent = node.toString(),
            content = ContentBuilder.getItemMetaContent(
                imageOriginal = node.getText(*FIELD_IMAGE_ORIGINAL),
                imageBig = node.getText(*FIELD_IMAGE_PREVIEW),
                imagePreview = node.getText(*FIELD_IMAGE_BIG),
                videoOriginal = node.getText(*FIELD_VIDEO_ORIGINAL)
            )
        )
    }
}
