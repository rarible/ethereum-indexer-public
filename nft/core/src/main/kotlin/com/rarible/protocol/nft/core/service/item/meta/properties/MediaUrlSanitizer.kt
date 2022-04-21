package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.BASE_64_SVG_PREFIX
import com.rarible.protocol.nft.core.service.item.meta.descriptors.UTF8_SVG_PREFIX
import com.rarible.protocol.nft.core.service.item.meta.descriptors.base64MimeToBytes

object MediaUrlSanitizer {

    fun sanitize(properties: ItemProperties): ItemProperties {
        return properties.copy(
            image = sanitize(properties.image),
            imageBig = sanitize(properties.imageBig),
            imagePreview = sanitize(properties.imagePreview),
            animationUrl = sanitize(properties.animationUrl),
        )
    }

    private fun sanitize(url: String?): String? {
        if (url == null) {
            return null
        }
        if (url.startsWith(BASE_64_SVG_PREFIX)) {
            return String(base64MimeToBytes(url.removePrefix(BASE_64_SVG_PREFIX)))
        }
        if (url.startsWith(UTF8_SVG_PREFIX)) {
            return url.removePrefix(UTF8_SVG_PREFIX)
        }
        return url
    }
}