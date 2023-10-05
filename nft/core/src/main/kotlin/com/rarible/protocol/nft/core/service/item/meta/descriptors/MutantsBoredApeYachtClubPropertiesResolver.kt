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
class MutantsBoredApeYachtClubPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient
) : ItemPropertiesResolver {

    override val name get() = "Bored"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != MUTANTS_BAYC_ADDRESS) return null

        logMetaLoading(itemId, "Resolving MutantApeYachtClub properties")
        val url = "$MUTANTS_URL/${itemId.tokenId}"
        val rawProperties = externalHttpClient.getBody(url = url, useProxy = true, id = itemId.decimalStringValue) ?: return null

        return try {
            logMetaLoading(itemId, "parsing properties by URI: $url")

            val json = JsonPropertiesParser.parse(itemId, rawProperties)
            json?.let { map(itemId, json, rawProperties) }
        } catch (e: Throwable) {
            logMetaLoading(itemId, "failed to parse properties by URI: $url", warn = true)
            null
        }
    }

    private fun map(itemId: ItemId, json: ObjectNode, rawProperties: String) =
        ItemProperties(
            name = "MutantApeYachtClub #${itemId.tokenId.value}",
            description = "The MUTANT APE YACHT CLUB is a collection of up to 20,000 Mutant Apes that can only be created by exposing an existing Bored Ape to a vial of MUTANT SERUM or by minting a Mutant Ape in the public sale.",
            attributes = json.withArray("attributes")
                .map { attr ->
                    ItemAttribute(
                        key = attr.path("trait_type").asText(),
                        value = attr.path("value").asText()
                    )
                },
            rawJsonContent = rawProperties,
            content = ContentBuilder.getItemMetaContent(
                imageOriginal = json.path("image").asText(),
            )
        )

    companion object {
        private const val MUTANTS_URL = "https://boredapeyachtclub.com/api/mutants/"
        val MUTANTS_BAYC_ADDRESS: Address = Address.apply("0x60e4d786628fea6478f785a6d7e704777c86a7c6")
    }
}
