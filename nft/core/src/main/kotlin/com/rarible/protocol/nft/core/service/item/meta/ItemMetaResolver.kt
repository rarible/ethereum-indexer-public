package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemContentMeta
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import org.springframework.stereotype.Component

/**
 * Resolves items' metadata consisting of [ItemPropertiesService] and [MediaMetaService].
 */
@Component
class ItemMetaResolver(
    private val itemPropertiesService: ItemPropertiesService,
    private val rariblePropertiesResolver: RariblePropertiesResolver,
    private val mediaMetaService: MediaMetaService // TODO Remove
) {

    suspend fun resolveItemMeta(itemId: ItemId): ItemMeta? {
        val itemProperties = itemPropertiesService.resolve(itemId) ?: return null
        val contentMeta = getCachedContentMeta(itemProperties, itemId.decimalStringValue)
        return ItemMeta(itemProperties.fixAnimationUrl(), contentMeta)
    }

    suspend fun resolvePendingItemMeta(itemId: ItemId, tokenUri: String): ItemMeta? {
        val itemProperties = rariblePropertiesResolver.resolveByTokenUri(itemId, tokenUri) ?: return null
        // Meta content will be resolved on the union-service.
        return ItemMeta(itemProperties, ItemContentMeta(null, null))
    }

    // TODO[meta]: Ethereum API will be not responsible for loading metadata for items.
    //  Metadata will be loaded and cached on the Multichain API (union-service)
    //  We only return from cache what we have already collected in the Ethereum NFT database.
    private suspend fun getCachedContentMeta(itemProperties: ItemProperties, id: String): ItemContentMeta {
        val imageMediaMeta = when {
            itemProperties.imagePreview != null -> mediaMetaService.getMediaMetaFromCache(itemProperties.imagePreview, id)
            itemProperties.image != null -> mediaMetaService.getMediaMetaFromCache(itemProperties.image, id)
            else -> null
        }
        val animationMediaMeta = when {
            itemProperties.animationUrl != null -> mediaMetaService.getMediaMetaFromCache(itemProperties.animationUrl, id)
            else -> null
        }
        return ItemContentMeta(imageMediaMeta, animationMediaMeta)
    }

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
