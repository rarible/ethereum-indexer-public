package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class LuckyManekiNftPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient,
) : ItemPropertiesResolver {

    override val name get() = "LuckyManekiNft"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != LUCKY_MANEKI_NFT_ADDRESS) return null

        logMetaLoading(itemId, "Resolving $name properties")
        val url = "$LUCKY_MANEKI_NFT_URL/${itemId.tokenId.value}"
        val rawProperties = externalHttpClient.getBody(url = url, useProxy = true, id = itemId.decimalStringValue) ?: return null

        return try {
            logMetaLoading(itemId, "parsing properties by URI: $url")

            val json = JsonPropertiesParser.parse(itemId, rawProperties)
            map(itemId, json, rawProperties)
        } catch (e: Throwable) {
            logMetaLoading(itemId, "failed to parse properties by URI: $url", warn = true)
            null
        }
    }

    private fun map(itemId: ItemId, json: ObjectNode, rawProperties: String) =
        ItemProperties(
            name = "Lucky Maneki #${itemId.tokenId.value}",
            description = null,
            attributes = json.withArray("attributes")
                .map { attr ->
                    ItemAttribute(
                        key = attr.path("trait_type").asText(),
                        value = attr.path("value").asText()
                    )
                },
            rawJsonContent = rawProperties,
            content = ContentBuilder.getItemMetaContent(
                imageOriginal = json.path("image").asText()
            )
        )

    companion object {
        private const val LUCKY_MANEKI_NFT_URL = "https://lucky-maneki-metadata.s3.amazonaws.com/token/"
        val LUCKY_MANEKI_NFT_ADDRESS: Address = Address.apply("0x14f03368b43e3a3d27d45f84fabd61cc07ea5da3")
    }
}
