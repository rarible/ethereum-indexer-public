package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.TOKEN_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.getInt
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.Companion.logProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class OpenseaTokenPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient,
    @Value("\${api.opensea.url:}") private val openseaUrl: String
) : TokenPropertiesResolver {

    override suspend fun resolve(id: Address): TokenProperties? {
        if (openseaUrl.isBlank()) return null

        val url = "${openseaUrl}/asset_contract/${id.prefixed()}"
        logProperties(id, "OpenSea: getting properties from $url")

        val rawProperties = externalHttpClient.getBody(url = url, id = id.prefixed()) ?: return null

        return try {
            logProperties(id, "parsing properties by URI: $url")

            val json = JsonPropertiesParser.parse(id.prefixed(), rawProperties)
            map(json)
        } catch (e: Error) {
            val errorMessage = "failed to parse properties by URI: $url"
            logProperties(id, errorMessage, true)

            throw e
        }
    }

    override val order get() = Int.MIN_VALUE

    private fun map(json: ObjectNode): TokenProperties {
        return TokenProperties(
            name = getName(json) ?: TokenProperties.EMPTY.name,
            description = json.getText("description"),
            externalUri = json.getText("external_link"),
            sellerFeeBasisPoints = json.getInt("opensea_seller_fee_basis_points"),
            feeRecipient = json.getText("payout_address")?.let { if (it.isNotBlank()) Address.apply(it) else null },
            content = ContentBuilder.getTokenMetaContent(
                imageOriginal = json.getText("image_url")
            )
        )
    }

    private fun getName(node: ObjectNode): String? {
        val name = node.getText("name")
        return if (name == OPEN_SEA_COLLECTION_DEFAULT_NAME) {
            node.get("collection")?.getText("name") ?: name
        } else {
            name
        }
    }

    private companion object {
        const val OPEN_SEA_COLLECTION_DEFAULT_NAME = "Unidentified contract"
    }
}
