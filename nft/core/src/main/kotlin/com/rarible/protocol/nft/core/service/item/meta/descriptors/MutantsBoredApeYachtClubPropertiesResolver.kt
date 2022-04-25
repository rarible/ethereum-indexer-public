package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesWrapper
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import scalether.domain.Address

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class MutantsBoredApeYachtClubPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient
) : ItemPropertiesResolver {

    private val mapper = ObjectMapper()

    override val name get() = "Bored"

    override suspend fun resolve(itemId: ItemId): ItemPropertiesWrapper {
        if (itemId.token != MUTANTS_BAYC_ADDRESS) {
            return wrapAsUnResolved(null)
        }
        logMetaLoading(itemId, "Resolving MutantApeYachtClub properties")
        val properties = externalHttpClient.get("$MUTANTS_URL/${itemId.tokenId}", useProxy = true)
            .bodyToMono<String>()
            .map { jsonContent ->
                val node = mapper.readTree(jsonContent) as ObjectNode
                ItemProperties(
                    name = "MutantApeYachtClub #${itemId.tokenId.value}",
                    description = "The MUTANT APE YACHT CLUB is a collection of up to 20,000 Mutant Apes that can only be created by exposing an existing Bored Ape to a vial of MUTANT SERUM or by minting a Mutant Ape in the public sale.",
                    image = node.path("image").asText(),
                    imagePreview = null,
                    imageBig = null,
                    animationUrl = null,
                    attributes = node.withArray("attributes")
                        .map { attr ->
                            ItemAttribute(
                                key = attr.path("trait_type").asText(),
                                value = attr.path("value").asText()
                            )
                        },
                    rawJsonContent = jsonContent
                )
            }.awaitFirstOrNull()

        return wrapAsResolved(properties)
    }

    companion object {
        private const val MUTANTS_URL = "https://boredapeyachtclub.com/api/mutants/"
        val MUTANTS_BAYC_ADDRESS: Address = Address.apply("0x60e4d786628fea6478f785a6d7e704777c86a7c6")
    }
}
