package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.contracts.erc1155.v1.rarible.RaribleToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService.Companion.logProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.transaction.MonoTransactionSender
import java.time.Duration

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class RariblePropertiesResolver(
    private val sender: MonoTransactionSender,
    private val tokenRepository: TokenRepository,
    private val ipfsService: IpfsService,
    private val externalHttpClient: ExternalHttpClient,
    @Value("\${api.properties.request-timeout}") requestTimeout: Long
) : ItemPropertiesResolver {

    private val timeout = Duration.ofMillis(requestTimeout)

    private val mapper = ObjectMapper().registerKotlinModule()

    override val name get() = "Rarible"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        val tokenUri = getUri(itemId)
        if (tokenUri.isNullOrBlank()) {
            logProperties(itemId, "empty token URI", warn = true)
            return null
        }
        logProperties(itemId, "got URI from token contract: $tokenUri")
        return resolveByTokenUri(itemId, tokenUri)
    }

    suspend fun resolveByTokenUri(itemId: ItemId, tokenUri: String): ItemProperties? {
        if ("{id}" in tokenUri) {
            logProperties(itemId, "got token URI with {id} placeholder: $tokenUri")
            val substitutions = listOf(
                itemId.tokenId.toString().removePrefix("0x"),
                itemId.tokenId.toString(),
                itemId.tokenId.value.toString()
            )
            for (substitution in substitutions) {
                val fixedTokenUri = tokenUri.replace("{id}", substitution)
                val itemProperties = resolve(itemId, fixedTokenUri)
                if (itemProperties != null) {
                    logProperties(itemId, "substitution of {id} with $fixedTokenUri did work")
                    return itemProperties
                }
            }
        }
        return resolve(itemId, tokenUri)
    }

    private suspend fun resolve(itemId: ItemId, tokenUri: String): ItemProperties? {
        if (tokenUri.startsWith(BASE_64_JSON_PREFIX)) {
            return parseFromBase64(itemId, tokenUri.removePrefix(BASE_64_JSON_PREFIX))?.fixEmptyName(itemId)
        }
        return getByUri(itemId, tokenUri)?.fixEmptyName(itemId)
    }

    private suspend fun ItemProperties.fixEmptyName(itemId: ItemId): ItemProperties {
        if (name.isNotBlank()) {
            return this
        }
        val collectionName = try {
            getCollectionName(itemId)
        } catch (e: Exception) {
            logProperties(itemId, "unable to fetch collection name: ${e.message}", warn = true)
            return this
        }
        val tokenId = "#${itemId.tokenId.value}"
        val newName = if (collectionName.isNullOrBlank()) tokenId else "$collectionName $tokenId"
        return copy(name = newName)
    }

    private suspend fun parseFromBase64(itemId: ItemId, uri: String): ItemProperties? {
        logProperties(itemId, "parsing properties as Base64")
        val decodedJson = try {
            String(base64MimeToBytes(uri))
        } catch (e: Exception) {
            logProperties(itemId, "failed to decode Base64: ${e.message}", warn = true)
            return null
        }
        return parseJsonProperties(itemId, decodedJson)
    }

    private suspend fun getByUri(itemId: ItemId, uri: String): ItemProperties? {
        val httpUrl = ipfsService.resolveHttpUrl(uri)
        logProperties(itemId, "getting properties by URI: $uri resolved as HTTP $httpUrl")
        val clientSpec = try {
            externalHttpClient.get(httpUrl)
        } catch (e: Exception) {
            logProperties(itemId, "failed to parse URI: $httpUrl: ${e.message}", warn = true)
            return null
        }
        return clientSpec
            .bodyToMono<String>()
            .timeout(timeout)
            .onErrorResume {
                logProperties(itemId, "failed to get properties by URI $httpUrl: ${it.message}", warn = true)
                Mono.empty()
            }
            .flatMap {
                logProperties(itemId, "parsing properties by URI: $httpUrl")
                if (it.length > 1_000_000) {
                    logProperties(itemId, "suspiciously big item properties ${it.length} for $httpUrl", warn = true)
                }
                mono { parseJsonProperties(itemId, it) }
            }
            .onErrorResume {
                logProperties(itemId, "failed to parse properties by URI: $httpUrl", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }

    private suspend fun parseJsonProperties(itemId: ItemId, jsonBody: String): ItemProperties? {
        @Suppress("BlockingMethodInNonBlockingContext")
        val node = try {
            mapper.readTree(jsonBody) as ObjectNode
        } catch (e: Exception) {
            logProperties(itemId, "failed to parse properties from json: ${e.message}", warn = true)
            return null
        }
        val imageUrl = node.getText("image", "image_url", "image_content", "image_data")?.let { parseImageUrl(it) }
        val imagePreview = node.getText("imagePreview", "image_preview", "image_preview_url")?.let { parseImageUrl(it) }
        val imageBig = node.getText("imageBig", "image_big", "image_big_url")?.let { parseImageUrl(it) }
        val animationUrl = node.getText("animation", "animation_url", "animationUrl")?.let { parseImageUrl(it) }
        return ItemProperties(
            name = node.getText("name", "label", "title") ?: "",
            description = node.getText("description"),
            image = imageUrl,
            imagePreview = imagePreview,
            imageBig = imageBig,
            animationUrl = animationUrl,
            attributes = node.parseAttributes(),
            rawJsonContent = node.toString()
        )
    }

    private suspend fun parseImageUrl(imageUrl: String): String {
        if (imageUrl.startsWith(BASE_64_SVG_PREFIX)) {
            return ipfsService.upload(
                fileName = "image.svg",
                someByteArray = base64MimeToBytes(imageUrl.removePrefix(BASE_64_SVG_PREFIX)),
                contentType = "image/svg+xml"
            )
        }
        if (imageUrl.startsWith("<svg") && imageUrl.endsWith("</svg>")) {
            return ipfsService.upload(
                fileName = "image.svg",
                someByteArray = imageUrl.toByteArray(),
                contentType = "image/svg+xml"
            )
        }
        return imageUrl
    }

    private suspend fun getUri(itemId: ItemId): String? {
        val token = tokenRepository.findById(itemId.token).awaitFirstOrNull()
        if (token == null) {
            logProperties(itemId, "token is not found", warn = true)
            return null
        }
        return when (token.standard) {
            TokenStandard.ERC1155 -> getErc1155TokenUri(itemId)
            TokenStandard.ERC721 -> getErc721TokenUri(itemId)
            else -> null
        }
    }

    private suspend fun getCollectionName(itemId: ItemId): String? {
        val token = tokenRepository.findById(itemId.token).awaitFirstOrNull() ?: return null
        @Suppress("ReactiveStreamsUnusedPublisher")
        return when (token.standard) {
            TokenStandard.ERC1155 -> RaribleToken(itemId.token, sender).name()
            TokenStandard.ERC721 -> MintableToken(itemId.token, sender).name()
            else -> Mono.empty()
        }.onErrorResume {
            logProperties(itemId, "failed to get name() from contract: ${it.message}", warn = true)
            Mono.empty()
        }.awaitFirstOrNull()
    }

    private suspend fun getErc1155TokenUri(itemId: ItemId): String? {
        return RaribleToken(itemId.token, sender)
            .uri(itemId.tokenId.value)
            .timeout(timeout)
            .onErrorResume {
                logProperties(itemId, "failed to get 'uri' from contract: ${it.message}", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }

    private suspend fun getErc721TokenUri(itemId: ItemId): String? {
        return MintableToken(itemId.token, sender)
            .tokenURI(itemId.tokenId.value)
            .timeout(timeout)
            .onErrorResume {
                logProperties(itemId, "failed get 'tokenURI' from contract: ${it.message}", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RariblePropertiesResolver::class.java)
    }
}
