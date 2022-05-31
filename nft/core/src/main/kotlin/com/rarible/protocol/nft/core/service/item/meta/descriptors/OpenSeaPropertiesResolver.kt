package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.meta.resource.http.PropertiesHttpLoader
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.math.BigInteger

@Service
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class OpenSeaPropertiesResolver(
    private val propertiesHttpLoader: PropertiesHttpLoader,
    private val properties: NftIndexerProperties
) : ItemPropertiesResolver {

    private val defaultImageUrlParser = DefaultOpenSeaImageUrlParser(properties.blockchain)

    override val name get() = "OpenSea"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        return resolve(itemId, defaultImageUrlParser)
    }

    suspend fun resolve(itemId: ItemId, imageUrlParser: OpenSeaImageUrlParser): ItemProperties? {
        if (propertiesHttpLoader.getOpenSeaUrl().isBlank()) return null
        val url = createOpenSeaUrl(itemId)
        logMetaLoading(itemId, "OpenSea: getting properties from $url")
        val propertiesString = propertiesHttpLoader.getBody(url = url,  id = itemId.decimalStringValue) ?: return null

        return try {
            logMetaLoading(itemId, "parsing properties by URI: $url")

            val json = JsonPropertiesParser.parse(itemId, propertiesString)
            json?.let { map(itemId, json, imageUrlParser.parseImage(json)) }
        } catch (e: Throwable) {
            val errorMessage = if (e is WebClientResponseException) " ${e.rawStatusCode}: ${e.statusText}" else ""
            logMetaLoading(itemId, "OpenSea: failed to get properties $errorMessage", warn = true)
            null
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
            image = image.ifNotBlank()?.replace(
                "{id}",
                itemId.tokenId.toString()
            ),
            imagePreview = jsonBody.getText("image_preview_url").ifNotBlank(),
            imageBig = jsonBody.getText("image_url").ifNotBlank(),
            animationUrl = jsonBody.getText("animation_url").ifNotBlank(),
            attributes = jsonBody.parseAttributes(),
            rawJsonContent = null
        )

    private fun createOpenSeaUrl(itemId: ItemId): String {
        return when (properties.blockchain) {
            Blockchain.ETHEREUM -> "${propertiesHttpLoader.getOpenSeaUrl()}/asset/${itemId.token}/${itemId.tokenId.value}/"
            Blockchain.POLYGON -> "${propertiesHttpLoader.getOpenSeaUrl()}/metadata/matic/${itemId.token}/${itemId.tokenId.value}"
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
            Blockchain.POLYGON -> node.getText("image")
        }
    }
}
