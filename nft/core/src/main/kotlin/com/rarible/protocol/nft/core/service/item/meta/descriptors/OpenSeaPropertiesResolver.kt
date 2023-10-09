package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.common.ifNotBlank
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.parseAttributes
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.math.BigInteger

@Service
class OpenSeaPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient,
    private val properties: NftIndexerProperties,
) : ItemPropertiesResolver {

    private val defaultImageUrlParser = DefaultOpenSeaImageUrlParser(properties.blockchain)

    override val name get() = "OpenSea"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        return resolve(itemId, defaultImageUrlParser)
    }

    suspend fun resolve(itemId: ItemId, imageUrlParser: OpenSeaImageUrlParser): ItemProperties? {
        if (properties.opensea.url.isBlank()) return null
        val url = createOpenSeaUrl(itemId)
        logMetaLoading(itemId, "OpenSea: getting properties from $url")
        val rawProperties = externalHttpClient.getBody(url = url, id = itemId.decimalStringValue) ?: return null

        return try {
            logMetaLoading(itemId, "parsing properties by URI: $url")

            val json = JsonPropertiesParser.parse(itemId, rawProperties)
            val result = map(itemId, json, imageUrlParser.parseImage(json))
            // There is no sense to return meta with name only
            // TODO ideally we should get rid of OpenSea meta at all
            if (result != null && result.copy(name = "").isEmpty()) {
                logMetaLoading(itemId, "empty meta json received from OpenSea URI: $url")
                null
            } else {
                result
            }
        } catch (e: Throwable) {
            val errorMessage = if (e is WebClientResponseException) " ${e.rawStatusCode}: ${e.statusText}" else ""
            logMetaLoading(itemId, "OpenSea: failed to get properties $errorMessage", warn = true)
            throw e
        }
    }

    private fun map(
        itemId: ItemId,
        jsonBody: ObjectNode,
        image: String?
    ): ItemProperties =
        ItemProperties(
            name = parseName(jsonBody, itemId.tokenId.value),
            description = jsonBody.getText("description"),
            attributes = jsonBody.parseAttributes(),
            rawJsonContent = null,
            content = ContentBuilder.getItemMetaContent(
                imageOriginal = image.ifNotBlank()?.replace("{id}", itemId.tokenId.toString()),
                imageBig = jsonBody.getText("image_preview_url").ifNotBlank(),
                imagePreview = jsonBody.getText("image_url").ifNotBlank(),
                videoOriginal = jsonBody.getText("animation_url").ifNotBlank(),
            )
        )

    private fun createOpenSeaUrl(itemId: ItemId): String {
        val openseaUrl = properties.opensea.url
        return when (properties.blockchain) {
            Blockchain.ETHEREUM -> "$openseaUrl/asset/${itemId.token}/${itemId.tokenId.value}/"
            Blockchain.POLYGON -> "$openseaUrl/metadata/matic/${itemId.token}/${itemId.tokenId.value}"
            Blockchain.OPTIMISM -> "$openseaUrl/metadata/optimism/${itemId.token}/${itemId.tokenId.value}"
            Blockchain.MANTLE,
            Blockchain.HEDERA -> throw IllegalStateException("OpenSea is not supported for ${properties.blockchain}")
        }
    }

    private fun parseName(node: ObjectNode, tokenId: BigInteger): String {
        return node.getText("name")
            ?: node.get("asset_contract")?.getText("name")?.let { "$it #$tokenId" }
            ?: "#$tokenId"
    }
}

interface OpenSeaImageUrlParser {

    fun parseImage(node: ObjectNode): String?
}

class DefaultOpenSeaImageUrlParser(
    private val blockchain: Blockchain
) : OpenSeaImageUrlParser {

    override fun parseImage(node: ObjectNode): String? {
        return when (blockchain) {
            Blockchain.ETHEREUM -> node.getText("image_original_url") ?: node.getText("image_url")
            Blockchain.POLYGON,
            Blockchain.OPTIMISM,
            Blockchain.MANTLE,
            Blockchain.HEDERA -> node.getText("image")
        }
    }
}
