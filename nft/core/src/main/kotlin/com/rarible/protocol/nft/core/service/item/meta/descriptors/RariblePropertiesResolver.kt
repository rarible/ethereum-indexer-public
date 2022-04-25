package com.rarible.protocol.nft.core.service.item.meta.descriptors

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
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ItemPropertiesUrlSanitizer
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
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

    override val name get() = "Rarible"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        val tokenUri = getUri(itemId)
        if (tokenUri.isNullOrBlank()) {
            logMetaLoading(itemId, "empty token URI", warn = true)
            return null
        }
        logMetaLoading(itemId, "got URI from token contract: $tokenUri")
        return resolveByTokenUri(itemId, tokenUri)
    }

    suspend fun resolveByTokenUri(itemId: ItemId, tokenUri: String): ItemProperties? {
        if ("{id}" in tokenUri) {
            logMetaLoading(itemId, "got token URI with {id} placeholder: $tokenUri")
            val substitutions = listOf(
                itemId.tokenId.toString().removePrefix("0x"),
                itemId.tokenId.toString(),
                itemId.tokenId.value.toString()
            )
            for (substitution in substitutions) {
                val fixedTokenUri = tokenUri.replace("{id}", substitution)
                val itemProperties = resolve(itemId, fixedTokenUri)
                if (itemProperties != null) {
                    logMetaLoading(itemId, "substitution of {id} with $fixedTokenUri did work")
                    return itemProperties
                }
            }
        }
        return resolve(itemId, tokenUri)
    }

    private suspend fun resolve(itemId: ItemId, tokenUri: String): ItemProperties? {
        // Sometimes there could be a json instead of URL
        val json = JsonPropertiesParser.parse(itemId, tokenUri)
        val properties = when {
            (json != null) -> JsonPropertiesMapper.map(itemId, json)
            else -> getByUri(itemId, tokenUri)
        } ?: return null

        val result = properties.fixEmptyName(itemId)
        return ItemPropertiesUrlSanitizer.sanitize(itemId, result)
    }

    private suspend fun ItemProperties.fixEmptyName(itemId: ItemId): ItemProperties {
        if (name.isNotBlank()) {
            return this
        }
        val collectionName = try {
            getCollectionName(itemId)
        } catch (e: Exception) {
            logMetaLoading(itemId, "unable to fetch collection name: ${e.message}", warn = true)
            return this
        }
        val tokenId = "#${itemId.tokenId.value}"
        val newName = if (collectionName.isNullOrBlank()) tokenId else "$collectionName $tokenId"
        return copy(name = newName)
    }

    private suspend fun getByUri(itemId: ItemId, uri: String): ItemProperties? {
        if (uri.isBlank()) {
            return null
        }

        val httpUrl = ipfsService.resolveInnerHttpUrl(uri)
        logMetaLoading(itemId, "getting properties by URI: $uri resolved as HTTP $httpUrl")
        val clientSpec = try {
            externalHttpClient.get(httpUrl)
        } catch (e: Exception) {
            logMetaLoading(itemId, "failed to parse URI: $httpUrl: ${e.message}", warn = true)
            return null
        }
        return clientSpec
            .bodyToMono<String>()
            .timeout(timeout)
            .onErrorResume {
                logMetaLoading(itemId, "failed to get properties by URI $httpUrl: ${it.message}", warn = true)
                Mono.empty()
            }
            .flatMap {
                logMetaLoading(itemId, "parsing properties by URI: $httpUrl")
                if (it.length > 1_000_000) {
                    logMetaLoading(itemId, "suspiciously big item properties ${it.length} for $httpUrl", warn = true)
                }
                mono {
                    val json = JsonPropertiesParser.parse(itemId, it)
                    json?.let { JsonPropertiesMapper.map(itemId, json) }
                }
            }
            .onErrorResume {
                logMetaLoading(itemId, "failed to parse properties by URI: $httpUrl", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }

    private suspend fun getUri(itemId: ItemId): String? {
        val token = tokenRepository.findById(itemId.token).awaitFirstOrNull()
        if (token == null) {
            logMetaLoading(itemId, "token is not found", warn = true)
            return null
        }
        val result = when (token.standard) {
            TokenStandard.ERC1155 -> getErc1155TokenUri(itemId)
            TokenStandard.ERC721 -> getErc721TokenUri(itemId)
            else -> null
        }
        return if (result.isNullOrBlank()) null else result
    }

    private suspend fun getCollectionName(itemId: ItemId): String? {
        val token = tokenRepository.findById(itemId.token).awaitFirstOrNull() ?: return null
        @Suppress("ReactiveStreamsUnusedPublisher")
        return when (token.standard) {
            TokenStandard.ERC1155 -> RaribleToken(itemId.token, sender).name()
            TokenStandard.ERC721 -> MintableToken(itemId.token, sender).name()
            else -> Mono.empty()
        }.onErrorResume {
            logMetaLoading(itemId, "failed to get name() from contract: ${it.message}", warn = true)
            Mono.empty()
        }.awaitFirstOrNull()
    }

    private suspend fun getErc1155TokenUri(itemId: ItemId): String? {
        return RaribleToken(itemId.token, sender)
            .uri(itemId.tokenId.value)
            .timeout(timeout)
            .onErrorResume {
                logMetaLoading(itemId, "failed to get 'uri' from contract: ${it.message}", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }

    private suspend fun getErc721TokenUri(itemId: ItemId): String? {
        return MintableToken(itemId.token, sender)
            .tokenURI(itemId.tokenId.value)
            .timeout(timeout)
            .onErrorResume {
                logMetaLoading(itemId, "failed get 'tokenURI' from contract: ${it.message}", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RariblePropertiesResolver::class.java)
    }
}
