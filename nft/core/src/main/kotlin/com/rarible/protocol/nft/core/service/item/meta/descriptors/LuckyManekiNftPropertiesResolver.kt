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
class LuckyManekiNftPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient
) : ItemPropertiesResolver {

    private val mapper = ObjectMapper()

    override val name get() = "LuckyManekiNft"

    override suspend fun resolve(itemId: ItemId): ItemPropertiesWrapper {
        if (itemId.token != LUCKY_MANEKI_NFT_ADDRESS) {
            return wrapAsUnResolved(null)
        }
        logMetaLoading(itemId, "Resolving Lucky Maneki Nft properties")

        val properties = externalHttpClient.get("$LUCKY_MANEKI_NFT_URL/${itemId.tokenId.value}", useProxy = true)
            .bodyToMono<String>()
            .map { jsonContent ->
                val node = mapper.readTree(jsonContent) as ObjectNode
                ItemProperties(
                    name = "Lucky Maneki #${itemId.tokenId.value}",
                    image = node.path("image").asText(),
                    description = null,
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
        private const val LUCKY_MANEKI_NFT_URL = "https://lucky-maneki-metadata.s3.amazonaws.com/token/"
        val LUCKY_MANEKI_NFT_ADDRESS: Address = Address.apply("0x14f03368b43e3a3d27d45f84fabd61cc07ea5da3")
    }
}
