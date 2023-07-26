package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemResolutionAbortedException
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class PegaxyPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient
) : ItemPropertiesResolver {

    override val name = "Pegaxy"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != PEGAXY_ADDRESS) {
            return null
        }
        val httpUrl = "https://api-apollo.pegaxy.io/v1/game-api/pega/${itemId.tokenId.value}"
        val rawProperties = externalHttpClient.getBody(url = httpUrl, id = itemId.toString()) ?: return null

        val result = try {
            logMetaLoading(itemId, "parsing properties by URI: $httpUrl")
            val json = JsonPropertiesParser.parse(itemId, rawProperties)
            json?.get("pega")?.let { node ->
                return ItemProperties(
                    name = node.getText("name") ?: "",
                    description = null,
                    attributes = listOfNotNull(
                        node.getText("gender")?.let { ItemAttribute("Gender", it) },
                        node.getText("bloodLine")?.let { ItemAttribute("Blood Line", it) },
                        node.getText("breedType")?.let { ItemAttribute("Breed Type", it) },
                    ),
                    rawJsonContent = node.toString(),
                    content = ContentBuilder.getItemMetaContent(
                        imageOriginal = node.get("design")?.getText("avatar")
                    )
                )
            }
        } catch (e: Error) {
            logMetaLoading(itemId, "failed to parse properties by URI: $httpUrl", warn = true)
            null
        }

        return result ?: throw ItemResolutionAbortedException()
    }

    companion object {
        val PEGAXY_ADDRESS = Address.apply("0xd50d167dd35d256e19e2fb76d6b9bf9f4c571a3e")
    }
}
