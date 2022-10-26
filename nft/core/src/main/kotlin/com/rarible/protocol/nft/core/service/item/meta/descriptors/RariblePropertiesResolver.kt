package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.common.ifNotBlank
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.BlockchainTokenUriResolver
import com.rarible.protocol.nft.core.service.item.meta.ITEM_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.MetaException
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ItemPropertiesParser
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonItemPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import com.rarible.protocol.nft.core.service.item.meta.properties.RawPropertiesProvider
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class RariblePropertiesResolver(
    private val urlService: UrlService,
    private val rawPropertiesProvider: RawPropertiesProvider,
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
                try {
                    val fixedTokenUri = tokenUri.replace("{id}", substitution)
                    val itemProperties = resolve(itemId, fixedTokenUri)

                    if (itemProperties != null) {
                        logMetaLoading(itemId, "substitution of {id} with $fixedTokenUri did work")
                        return itemProperties
                    }
                } catch (_: Exception) {
                }
            }
        }
        return resolve(itemId, tokenUri)
    }

    private suspend fun resolve(itemId: ItemId, tokenUri: String): ItemProperties? {
        // Sometimes there could be a json instead of URL
        val json = runCatching { JsonPropertiesParser.parse(itemId, tokenUri) }.getOrNull()
        val properties = when {
            json != null -> JsonItemPropertiesMapper.map(itemId, json)
            else -> getByUri(itemId, tokenUri)?.copy(tokenUri = tokenUri)
        } ?: return null

        return properties.fixEmptyName(itemId)
    }

    private suspend fun getByUri(itemId: ItemId, uri: String): ItemProperties? {
        uri.ifNotBlank() ?: return null
        val resource = urlService.parseUrl(uri, itemId.toString()) ?: throw MetaException(
            "$uri unparseable",
            status = MetaException.Status.UnparseableLink
        )
        val rawProperties = rawPropertiesProvider.getContent(itemId, resource) ?: return null

        return ItemPropertiesParser.parse(
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
