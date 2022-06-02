package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetectProcessor
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.UrlService
import org.springframework.stereotype.Component

@Component
class ItemPropertiesUrlSanitizer(
    private val urlService: UrlService,
    private val embeddedContentDetectProcessor: EmbeddedContentDetectProcessor
) {

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

        val embeddedContent = embeddedContentDetectProcessor.decode(url)
        if (embeddedContent != null) {
            return SvgSanitizer.sanitize(itemId, embeddedContent.content.decodeToString())
        }

        val resource = urlService.parseResource(url)
        if (resource is ArweaveUrl) {
            return urlService.resolvePublicHttpUrl(url, itemId.decimalStringValue)
        }

        return url  // TODO  Should return bad Url?
    }
}
