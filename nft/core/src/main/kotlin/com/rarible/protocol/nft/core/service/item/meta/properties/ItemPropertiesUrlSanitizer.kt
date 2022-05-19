package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties

object ItemPropertiesUrlSanitizer {

    fun sanitize(itemId: ItemId, properties: ItemProperties): ItemProperties {
        return properties.copy(
            image = sanitize(itemId, properties.image),
            imageBig = sanitize(itemId, properties.imageBig),
            imagePreview = sanitize(itemId, properties.imagePreview),
            animationUrl = sanitize(itemId, properties.animationUrl),
        )
    }

    private fun sanitize(itemId: ItemId, url: String?): String? {
        if (url == null) {
            return null
        }

        val fixedUrl = ShortUrlResolver.resolve(url)

        val svg = SvgSanitizer.sanitize(itemId, fixedUrl)

        return svg ?: fixedUrl
    }
}
