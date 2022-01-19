package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemContentMeta
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import org.springframework.stereotype.Component

/**
 * Resolves items' metadata consisting of [ItemPropertiesService] and [MediaMetaService].
 */
@Component
class ItemMetaResolver(
    private val itemPropertiesService: ItemPropertiesService,
    private val mediaMetaService: MediaMetaService
) {

    suspend fun resolveItemMeta(itemId: ItemId): ItemMeta? {
        val itemProperties = itemPropertiesService.resolve(itemId) ?: return null
        val contentMeta = loadContentMeta(itemProperties)
        return ItemMeta(itemProperties.fixAnimationUrl(), contentMeta)
    }

    private suspend fun loadContentMeta(itemProperties: ItemProperties): ItemContentMeta {
        val imageMediaMeta = when {
            itemProperties.imagePreview != null -> mediaMetaService.getMediaMeta(itemProperties.imagePreview)
            itemProperties.image != null -> mediaMetaService.getMediaMeta(itemProperties.image)
            else -> null
        }
        val animationMediaMeta = when {
            itemProperties.animationUrl != null -> mediaMetaService.getMediaMeta(itemProperties.animationUrl)
            else -> null
        }
        return ItemContentMeta(imageMediaMeta, animationMediaMeta)
    }

    // TODO[meta]: this fix may be re-implemented using content meta requested with mediaMetaService.getMediaMeta(...)
    private fun ItemProperties.fixAnimationUrl(): ItemProperties {
        fun String?.hasAnimationExtension() =
            ANIMATION_EXTENSIONS.any { this?.endsWith(it) == true }

        val isWrongImage = image.hasAnimationExtension()
        val isWrongImageBig = imageBig.hasAnimationExtension()
        val isWrongImagePreview = imagePreview.hasAnimationExtension()
        if (animationUrl.isNullOrBlank() && (isWrongImage || isWrongImageBig || isWrongImagePreview)) {
            return copy(
                image = image.takeUnless { isWrongImage },
                imageBig = imageBig.takeUnless { isWrongImageBig },
                imagePreview = imagePreview.takeUnless { isWrongImagePreview },
                animationUrl = when {
                    isWrongImage -> image
                    isWrongImageBig -> imageBig
                    isWrongImagePreview -> imagePreview
                    else -> animationUrl
                }
            )
        }
        return this
    }

    companion object {
        private val ANIMATION_EXTENSIONS = listOf(".mov", ".mp4")
    }
}
