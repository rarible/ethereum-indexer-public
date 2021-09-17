package com.rarible.protocol.nft.api.service.item.meta

import com.rarible.protocol.nft.core.model.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.jboss.logging.Logger
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Component
class ItemMetaServiceImpl(
    private val itemPropertiesService: ItemPropertiesService,
    private val contentMetaService: ContentMetaService
) : ItemMetaService {

    private val logger = Logger.getLogger(this::class.java)

    override suspend fun getItemMetadata(itemId: ItemId): ItemMeta {
        return itemPropertiesService.getProperties(itemId.token, itemId.tokenId.value)
            .map { properties ->
                fixPropertiesIfNeeded(properties)
            }
            .onErrorResume {
                logger.warn("Unable to load item properties for $itemId: $it")
                Mono.just(FAKE_PROPS)
            }
            .switchIfEmpty {
                logger.warn("properties not found for $itemId")
                Mono.just(FAKE_PROPS)
            }
            .flatMap { properties ->
                contentMetaService.getByProperties(properties)
                    .map { meta ->
                        ItemMeta(properties, meta)
                    }
            }.awaitFirst()
    }

    override suspend fun resetMetadata(itemId: ItemId) {
        try {
            val itemMeta = getItemMetadata(itemId)
            contentMetaService.resetByProperties(itemMeta.properties).awaitFirstOrNull()
        } finally {
            itemPropertiesService.resetProperties(itemId.token, itemId.tokenId.value).awaitFirstOrNull()
        }
    }

    private fun fixPropertiesIfNeeded(properties: ItemProperties): ItemProperties {
        val image = properties.image
        val imageBig = properties.imageBig
        val imagePreview = properties.imagePreview
        val animationUrl = properties.animationUrl

        val isWrongImage = ANIMATION_EXPANSIONS.any { image?.endsWith(it, true) == true }
        val isWrongImageBig = ANIMATION_EXPANSIONS.any { imageBig?.endsWith(it, true) == true }
        val isWrongImagePreview = ANIMATION_EXPANSIONS.any { imagePreview?.endsWith(it, true) == true }

        return if (animationUrl.isNullOrBlank() && (isWrongImage || isWrongImageBig || isWrongImagePreview)) {
            properties.copy(
                image = if (isWrongImage || image?.startsWith("ipfs://") == true) null else image,
                imageBig = if (isWrongImageBig) null else imageBig,
                imagePreview = if (isWrongImagePreview) null else imagePreview,
                animationUrl = when {
                    isWrongImage -> image
                    isWrongImageBig -> imageBig
                    isWrongImagePreview -> imagePreview
                    else -> animationUrl
                }
            )
        } else {
            properties
        }
    }

    companion object {
        val ANIMATION_EXPANSIONS = setOf(".mov", ".mp4")
        val FAKE_PROPS = ItemProperties(name = "Untitled", description = null, image = null, imagePreview = null, imageBig = null, attributes = listOf())
    }
}
