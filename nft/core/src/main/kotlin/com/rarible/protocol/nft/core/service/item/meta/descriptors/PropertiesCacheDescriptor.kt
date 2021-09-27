package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import com.rarible.protocol.contracts.erc1155.v1.rarible.RaribleToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.service.item.meta.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.time.Duration

@Component
class PropertiesCacheDescriptor(
    private val sender: MonoTransactionSender,
    private val tokenRepository: TokenRepository,
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val ipfsService: IpfsService,
    @Value("\${api.properties.cache-timeout}") private val cacheTimeout: Long,
    @Value("\${api.properties.request-timeout}") private val requestTimeout: Long
) : CacheDescriptor<ItemProperties> {

    private val client = WebClient.builder().apply {
        DefaultProtocolWebClientCustomizer().customize(it)
    }.build()

    private val mapper = ObjectMapper().registerKotlinModule()

    override val collection: String = "cache_properties"
    override fun getMaxAge(value: ItemProperties?): Long =
        if (value == null) {
            DateUtils.MILLIS_PER_HOUR
        } else {
            cacheTimeout
        }

    override fun get(id: String): Mono<ItemProperties> {
        return LoggingUtils.withMarker { marker ->
            logger.info(marker, "get properties $id")

            id.parseTokenId()
                .let { (address, tokenId) -> getUri(address, tokenId).map { it.replace("{id}", EthUInt256.of(tokenId).toString()) } }
                .flatMap { uri ->
                    logger.info(marker, "got uri: $uri")
                    when {
                        isBase64String(uri) -> getFromBase64(uri)
                        uri.isNotEmpty() -> getByUri(uri)
                        else -> {
                            logger.warn(marker, "unable to get metadata for $id: token URI is empty")
                            Mono.empty()
                        }
                    }
                }
                .flatMap { fixEmptyName(id, it) }
                .timeout(Duration.ofMillis(requestTimeout))
                .onErrorResume {
                    logger.warn("Unable to get properties for $id", it)
                    Mono.empty()
                }
        }
    }

    fun fixEmptyName(id: String, item: ItemProperties): Mono<ItemProperties> = mono {
        when {
            item.name.isEmpty() -> {
                val (address, tokenId) = id.parseTokenId()
                val collection = getCollectionName(address, tokenId).awaitFirstOrNull()
                val name = collection?.let { "$it #$tokenId" } ?: "#$tokenId"
                item.copy(name = name)
            }
            else -> item
        }
    }

    fun getFromBase64(uri: String): Mono<ItemProperties> {
        val str = base64MimeToString(uri)
        logger.info("Decoding properties from base64: $str")
        return mono { parse(str) }
    }

    fun getByUri(uri: String): Mono<ItemProperties> {
        return client.get()
            .uri(ipfsService.resolveRealUrl(uri))
            .retrieve()
            .bodyToMono<String>()
            .flatMap {
                logger.info("Got properties from $uri")
                mono { parse(it) }
            }
    }

    private suspend fun parse(body: String): ItemProperties {
        val node = mapper.readTree(body) as ObjectNode
        val image = image(node)
        val animationUrl = node.getText("animation_url") ?: image
        return ItemProperties(
            name = node.getText("name", "label", "title") ?: "",
            description = node.getText("description"),
            image = image,
            imagePreview = null,
            imageBig = null,
            animationUrl = animationUrl,
            attributes = attributes(node)
        )
    }

    suspend fun image(node: ObjectNode): String {
        val imageNode = node.getText("image") ?: ""
        val imageUrl = node.getText("image", "image_url") ?: ""
        return when {
            imageNode?.startsWith("data:image/svg+xml;base64") -> {
                val hash = ipfsService.upload("image.svg", base64MimeToBytes(imageNode), "image/svg+xml")
                ipfsService.url(hash)
            }
            imageUrl.isNotEmpty() -> imageUrl
            imageNode.isNotEmpty() -> imageNode
            else -> ""
        }
    }

    fun attributes(node: ObjectNode): List<ItemAttribute> {
        return when {
            !node.path("attributes").isEmpty -> node.path("attributes").toProperties()
            !node.path("traits").isEmpty -> node.path("traits").toProperties()
            else -> emptyList()
        }
    }

    fun getUri(token: Address, tokenId: BigInteger): Mono<String> {
        //todo тест на получение lazy item properties
        return lazyNftItemHistoryRepository.findById(ItemId(token, EthUInt256(tokenId)))
            .map { it.uri }
            .switchIfEmpty {
                tokenRepository.findById(token).toOptional()
                    .flatMap {
                        if (it.isPresent && it.get().standard == TokenStandard.ERC1155) {
                            getErc1155TokenUri(token, tokenId)
                        } else {
                            getErc721TokenUri(token, tokenId)
                        }
                    }
            }
    }

    fun getCollectionName(token: Address, tokenId: BigInteger): Mono<String> {
        return tokenRepository.findById(token).toOptional()
            .flatMap {
                if (it.isPresent && it.get().standard == TokenStandard.ERC1155) {
                    RaribleToken(token, sender).name()
                } else {
                    MintableToken(token, sender).name()
                }
            }

    }

    private fun getErc1155TokenUri(token: Address, tokenId: BigInteger): Mono<String> {
        return RaribleToken(token, sender).uri(tokenId)
    }

    private fun getErc721TokenUri(token: Address, tokenId: BigInteger): Mono<String> {
        return MintableToken(token, sender).tokenURI(tokenId)
    }

    private fun convertObjectAttributes(attrs: ObjectNode): List<ItemAttribute> {
        return attrs.fields().asSequence()
            .mapNotNull { e ->
                ItemAttribute(e.key, e.value.asText())
            }
            .toList()
    }

    private fun convertArrayAttributes(attrs: ArrayNode): List<ItemAttribute> {
        return attrs.mapNotNull {
            val key = it.getText("key", "trait_type")
            if (key != null)
                ItemAttribute(key, it.getText("value"))
            else
                null
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PropertiesCacheDescriptor::class.java)
    }
}
