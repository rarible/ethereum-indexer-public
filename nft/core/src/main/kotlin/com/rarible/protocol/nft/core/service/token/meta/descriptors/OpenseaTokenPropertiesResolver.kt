package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonOpenSeaCollectionPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.Companion.logProperties
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class OpenseaTokenPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient,
    private val properties: NftIndexerProperties,
) : TokenPropertiesResolver {

    override val order get() = Int.MIN_VALUE

    override suspend fun resolve(collectionId: Address): TokenProperties? {
        if (properties.opensea.url.isBlank()) return null

        val url = "${properties.opensea.url}/asset_contract/${collectionId.prefixed()}"
        logProperties(collectionId, "OpenSea: getting Collection properties from $url")

        val rawProperties = externalHttpClient.getBody(url = url, id = collectionId.prefixed()) ?: return null

        try {
            logProperties(collectionId, "OpenSea: parsing Collection properties by URI: $url")

            val json = JsonPropertiesParser.parse(collectionId.prefixed(), rawProperties)
            return JsonOpenSeaCollectionPropertiesMapper.map(collectionId, json)
        } catch (e: Exception) {
            logProperties(collectionId, "OpenSea: failed to parse Collection properties by URI: $url", true)
            throw e
        }
    }
}
