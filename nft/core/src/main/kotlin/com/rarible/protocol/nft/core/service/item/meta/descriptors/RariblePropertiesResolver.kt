package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ItemPropertiesUrlSanitizer
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class RariblePropertiesResolver(
    private val ipfsService: IpfsService,
    private val propertiesHttpLoader: PropertiesHttpLoader,
    private val tokenUriResolver: BlockchainTokenUriResolver
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
            else -> getByUri(itemId, tokenUri)
        } ?: return null

        val result = properties.fixEmptyName(itemId)
        return ItemPropertiesUrlSanitizer.sanitize(itemId, result)
    }

    private suspend fun getByUri(itemId: ItemId, uri: String): ItemProperties? {
        if (uri.isBlank()) {
            return null
        }

        val httpUrl =  ipfsService.resolveInnerHttpUrl(uri)
        logMetaLoading(itemId, "getting properties by URI: $uri resolved as HTTP $httpUrl")

        val propertiesString = propertiesHttpLoader.getByUrl(itemId, httpUrl) ?: return null

        return try {
            logMetaLoading(itemId, "parsing properties by URI: $httpUrl")
            if (propertiesString.length > 1_000_000) {
                logMetaLoading(itemId, "suspiciously big item properties ${propertiesString.length} for $httpUrl", warn = true)
            }
            val json = JsonPropertiesParser.parse(itemId, propertiesString)
            json?.let { JsonPropertiesMapper.map(itemId, json) }
        } catch (e: Error) {
            logMetaLoading(itemId, "failed to parse properties by URI: $httpUrl", warn = true)
            null
        }
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
