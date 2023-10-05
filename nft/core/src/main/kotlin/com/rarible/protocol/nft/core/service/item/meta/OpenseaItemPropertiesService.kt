package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class OpenseaItemPropertiesService(
    private val itemPropertiesResolverProvider: ItemPropertiesResolverProvider,
    nftIndexerProperties: NftIndexerProperties
) {

    private val excludedFromOpenseaResolver = setOf(
        Address.apply(nftIndexerProperties.ensDomainsContractAddress)
    ) + nftIndexerProperties.excludedFromOpenseaMetaResolution.split(",")
        .filter { it.isNotBlank() }
        .map { Address.apply(it) }.toSet()

    suspend fun extendWithOpenSea(itemId: ItemId, itemProperties: ItemProperties): ItemProperties {
        // Workaround for preventing receiving dummy data from Opensea PT-422
        if (itemId.token in excludedFromOpenseaResolver) {
            return itemProperties
        }

        logMetaLoading(itemId, "resolving additional properties using OpenSea")
        val openSeaResult = resolveOpenSeaProperties(itemId) ?: return itemProperties
        return extendWithOpenSea(itemProperties, openSeaResult, itemId)
    }

    suspend fun fallbackToOpenSea(itemId: ItemId): ItemProperties? {
        logMetaLoading(itemId, "falling back to OpenSea")
        return resolveOpenSeaProperties(itemId)
    }

    private suspend fun resolveOpenSeaProperties(itemId: ItemId): ItemProperties? = try {
        itemPropertiesResolverProvider.openSeaResolver.resolve(itemId)
    } catch (e: Exception) {
        logMetaLoading(itemId, "unable to get properties from OpenSea: ${e.message}", warn = true)
        throw e
    }

    private fun extendWithOpenSea(
        rarible: ItemProperties,
        openSea: ItemProperties,
        itemId: ItemId
    ): ItemProperties {
        fun <T> extend(rarible: T, openSea: T, fieldName: String): T {
            if (openSea != null && rarible != openSea) {
                if (rarible is List<*> && openSea is List<*>) {
                    if (rarible.isNotEmpty() && openSea.isEmpty() ||
                        rarible.sortedBy { it.toString() } == openSea.sortedBy { it.toString() }
                    ) {
                        return rarible
                    }
                }
                if (fieldName == "name" &&
                    rarible is String &&
                    rarible.isNotEmpty() &&
                    openSea is String &&
                    openSea.endsWith("#${itemId.tokenId.value}")
                ) {
                    // Apparently, the OpenSea resolver has returned a dummy name as <collection name> #tokenId
                    // We don't want to override the Rarible resolver result.
                    return rarible
                }
                if (fieldName == "image" &&
                    rarible is String &&
                    rarible.isNotEmpty()
                ) {
                    // Sometimes OpenSea returns invalid original image URL.
                    return rarible
                }
                logMetaLoading(itemId, "extending $fieldName from OpenSea: rarible = [$rarible], openSea = [$openSea]")
                return openSea
            }
            return rarible
        }
        return ItemProperties(
            name = extend(rarible.name, openSea.name, "name"),
            description = extend(rarible.description, openSea.description, "description"),
            attributes = extend(rarible.attributes, openSea.attributes, fieldName = "attributes"),
            rawJsonContent = extend(rarible.rawJsonContent, openSea.rawJsonContent, "rawJsonContent"),
            content = ContentBuilder.getItemMetaContent(
                imageOriginal = extend(rarible.content.imageOriginal?.url, openSea.content.imageOriginal?.url, "image"),
                imageBig = extend(rarible.content.imageBig?.url, openSea.content.imageBig?.url, "imageBig"),
                imagePreview = extend(
                    rarible.content.imagePreview?.url, openSea.content.imagePreview?.url, "imagePreview"
                ),
                videoOriginal = extend(
                    rarible.content.videoOriginal?.url, openSea.content.videoOriginal?.url, "animationUrl"
                ),
            )
        )
    }
}
