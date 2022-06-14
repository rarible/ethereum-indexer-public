package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.common.ifNotBlank
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.BlockchainTokenUriResolver
import com.rarible.protocol.nft.core.service.item.meta.ITEM_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ItemPropertiesProvider
import com.rarible.protocol.nft.core.service.item.meta.properties.ItemPropertiesUrlSanitizer
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import com.rarible.protocol.nft.core.service.item.meta.properties.RawPropertiesProvider
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class RariblePropertiesResolver(
    private val urlService: UrlService,
    private val rawPropertiesProvider: RawPropertiesProvider,
    private val tokenUriResolver: BlockchainTokenUriResolver,
    private val itemPropertiesUrlSanitizer: ItemPropertiesUrlSanitizer
) : ItemPropertiesResolver {

    override val name get() = "Rarible"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        val tokenUri = tokenUriResolver.getUri(itemId)?.trim()
        if (tokenUri.isNullOrBlank()) {
            logMetaLoading(itemId, "empty token URI", warn = true)
            return null
        }
        logMetaLoading(itemId, "got URI from token contract: $tokenUri")
        return resolveByTokenUri(itemId, tokenUri)
    }

    suspend fun resolveByTokenUri(itemId: ItemId, tokenUri: String): ItemProperties? {
        if ("{id}" in tokenUri) {
            logMetaLoading(itemId, "got token URI with {id} placeholder: $tokenUri")
            val substitutions = listOf(
                itemId.tokenId.toString().removePrefix("0x"),
                itemId.tokenId.toString(),
                itemId.tokenId.value.toString()
            )
            for (substitution in substitutions) {
                val fixedTokenUri = tokenUri.replace("{id}", substitution)
                val itemProperties = resolve(itemId, fixedTokenUri)
                if (itemProperties != null) {
                    logMetaLoading(itemId, "substitution of {id} with $fixedTokenUri did work")
                    return itemProperties
                }
            }
        }
        return resolve(itemId, tokenUri)
    }

    private suspend fun resolve(itemId: ItemId, tokenUri: String): ItemProperties? {
        // Sometimes there could be a json instead of URL
        val json = JsonPropertiesParser.parse(itemId, tokenUri)
        val properties = when {
            (json != null) -> JsonPropertiesMapper.map(itemId, json)
            else -> getByUri(itemId, tokenUri)?.copy(tokenUri = tokenUri)
        } ?: return null

        val result = properties.fixEmptyName(itemId)
        return itemPropertiesUrlSanitizer.sanitize(itemId, result)
    }

    private suspend fun getByUri(itemId: ItemId, uri: String): ItemProperties? {
        uri.ifNotBlank() ?: return null
        val resource = urlService.parseUrl(uri, itemId.toString()) ?: return null
        val rawProperties = rawPropertiesProvider.getContent(itemId, resource) ?: return null

        return ItemPropertiesProvider.provide(
            itemId = itemId,
            httpUrl = urlService.resolveInternalHttpUrl(resource),
            rawProperties = rawProperties
        )
    }

    private suspend fun ItemProperties.fixEmptyName(itemId: ItemId): ItemProperties {
        if (name.isNotBlank()) {
            return this
        }
        val collectionName = try {
            tokenUriResolver.getCollectionName(itemId)
        } catch (e: Exception) {
            logMetaLoading(itemId, "unable to fetch collection name: ${e.message}", warn = true)
            return this
        }
        val tokenId = "#${itemId.tokenId.value}"
        val newName = if (collectionName.isNullOrBlank()) tokenId else "$collectionName $tokenId"
        return copy(name = newName)
    }
}
