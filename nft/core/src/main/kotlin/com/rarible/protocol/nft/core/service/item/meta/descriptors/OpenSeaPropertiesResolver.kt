package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.math.BigInteger
import java.time.Duration

@Service
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class OpenSeaPropertiesResolver(
    private val externalHttpClient: ExternalHttpClient,
    private val properties: NftIndexerProperties,
    @Value("\${api.opensea.request-timeout}") private val requestTimeout: Long,
) : ItemPropertiesResolver {

    private val defaultImageUrlParser = DefaultOpenSeaImageUrlParser(properties.blockchain)

    override val name get() = "OpenSea"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        return resolve(itemId, defaultImageUrlParser)
    }

    suspend fun resolve(itemId: ItemId, imageUrlParser: OpenSeaImageUrlParser): ItemProperties? {
        if (externalHttpClient.openseaUrl.isBlank()) return null
        val openSeaUrl = createOpenSeaUrl(itemId)
        logMetaLoading(itemId, "OpenSea: getting properties from $openSeaUrl")
        return externalHttpClient
            .get(openSeaUrl)
            .bodyToMono<ObjectNode>()
            .map {
                val image = imageUrlParser.parseImage(it)
                ItemProperties(
                    name = parseName(it, itemId.tokenId.value),
                    description = it.getText("description"),
                    image = image.ifNotBlank()?.replace(
                        "{id}",
                        itemId.tokenId.toString()
                    ),
                    imagePreview = it.getText("image_preview_url").ifNotBlank(),
                    imageBig = it.getText("image_url").ifNotBlank(),
                    animationUrl = it.getText("animation_url").ifNotBlank(),
                    attributes = it.parseAttributes(),
                    rawJsonContent = null
                )
            }
            .timeout(Duration.ofMillis(requestTimeout))
            .onErrorResume {
                logMetaLoading(
                    itemId,
                    "OpenSea: failed to get properties" + if (it is WebClientResponseException) {
                        " ${it.rawStatusCode}: ${it.statusText}"
                    } else {
                        ""
                    },
                    warn = true
                )
                Mono.empty()
            }
            .awaitFirstOrNull()
    }

    private fun createOpenSeaUrl(itemId: ItemId): String {
        return when (properties.blockchain) {
            Blockchain.ETHEREUM -> "${externalHttpClient.openseaUrl}/asset/${itemId.token}/${itemId.tokenId.value}/"
            Blockchain.POLYGON -> "${externalHttpClient.openseaUrl}/metadata/matic/${itemId.token}/${itemId.tokenId.value}"
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