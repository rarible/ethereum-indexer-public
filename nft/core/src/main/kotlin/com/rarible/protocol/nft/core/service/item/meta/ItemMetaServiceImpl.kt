package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ITEM_META_CAPTURE_SPAN_TYPE
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class ItemMetaServiceImpl(
    private val itemPropertiesService: ItemPropertiesService,
    private val mediaMetaService: MediaMetaService
) : ItemMetaService {

    override suspend fun getItemMetadata(itemId: ItemId, returnOnlyCachedMeta: Boolean): ItemMeta {
        val itemProperties = itemPropertiesService.resolve(itemId, returnOnlyCachedMeta) ?: return ItemMeta.EMPTY
        val contentMeta = getContentMetaForProperties(itemProperties)
        return ItemMeta(itemProperties.fixAnimationUrl(), contentMeta)
    }

    private suspend fun getContentMetaForProperties(itemProperties: ItemProperties): ContentMeta = coroutineScope {
        val imageMediaMeta = async {
            when {
                itemProperties.imagePreview != null -> mediaMetaService.getMediaMeta(itemProperties.imagePreview)
                itemProperties.image != null -> mediaMetaService.getMediaMeta(itemProperties.image)
                else -> null
            }
        }
        val animationMediaMeta = async {
            when {
                itemProperties.animationUrl != null -> mediaMetaService.getMediaMeta(itemProperties.animationUrl)
                else -> null
            }
        }
        return@coroutineScope ContentMeta(imageMediaMeta.await(), animationMediaMeta.await())
    }

    // TODO[meta]: this fix may be re-implemented using content meta requested with mediaMetaService.getMediaMeta(...)
    private fun ItemProperties.fixAnimationUrl(): ItemProperties {
        fun String?.hasAnimationExtension() = ANIMATION_EXTENSIONS.any { this?.endsWith(it) == true }
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

    override suspend fun resetMetadata(itemId: ItemId) {
        try {
            val itemProperties = itemPropertiesService.resolve(itemId)
            itemProperties?.image?.let { mediaMetaService.resetMediaMeta(it) }
            itemProperties?.imagePreview?.let { mediaMetaService.resetMediaMeta(it) }
            itemProperties?.imageBig?.let { mediaMetaService.resetMediaMeta(it) }
            itemProperties?.animationUrl?.let { mediaMetaService.resetMediaMeta(it) }
        } finally {
            itemPropertiesService.resetProperties(itemId)
        }
    }

    private companion object {
        private val ANIMATION_EXTENSIONS = listOf(".mov", ".mp4")
    }
}
