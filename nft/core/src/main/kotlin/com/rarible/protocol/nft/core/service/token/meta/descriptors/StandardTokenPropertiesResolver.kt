package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.BlockchainTokenUriResolver
import com.rarible.protocol.nft.core.service.item.meta.getInt
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.Companion.logProperties
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class StandardTokenPropertiesResolver(
    private val urlService: UrlService,
    private val externalHttpClient: ExternalHttpClient,
    private val tokenUriResolver: BlockchainTokenUriResolver
) : TokenPropertiesResolver {

    override suspend fun resolve(id: Address): TokenProperties? {
        val uri = tokenUriResolver.getCollectionUri(id)
        if (uri.isNullOrBlank()) {
            return null
        }

        val url = urlService.resolveInternalHttpUrl(uri) ?: return null
        logProperties(id, "$uri was resolved to: $url")
        return request(id, url)?.copy(tokenUri = uri)
    }

    override val order get() = Int.MAX_VALUE

    private suspend fun request(id: Address, url: String): TokenProperties? {
        val rawProperties = externalHttpClient.getBody(url = url, id = id.prefixed()) ?: return null

        return try {
            logProperties(id, "parsing properties by URI: $url")

            val json = JsonPropertiesParser.parse(id.prefixed(), rawProperties)
            map(json)
        } catch (e: Throwable) {
            logProperties(id, "failed to parse properties by URI: $url", warn = true)
            null
        }
    }

    private fun map(json: ObjectNode): TokenProperties {
        return TokenProperties(
            name = json.getText("name") ?: TokenProperties.EMPTY.name,
            description = json.getText("description"),
            externalUri = json.getText("external_link"),
            sellerFeeBasisPoints = json.getInt("seller_fee_basis_points"),
            feeRecipient = json.getText("fee_recipient")?.let { Address.apply(it) },
            content = ContentBuilder.getTokenMetaContent(
                imageOriginal = json.getText("image")
            )
        )
    }
}
