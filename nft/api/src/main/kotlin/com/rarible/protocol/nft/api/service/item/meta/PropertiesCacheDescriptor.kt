package com.rarible.protocol.nft.api.service.item.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import com.rarible.protocol.contracts.erc1155.v1.rarible.RaribleToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
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

@Component
class PropertiesCacheDescriptor(
    private val sender: MonoTransactionSender,
    private val tokenRepository: TokenRepository,
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val ipfsService: IpfsService,
    @Value("\${api.properties.cache-timeout}") private val cacheTimeout: Long
) : CacheDescriptor<ItemProperties> {

    private val client = WebClient.builder().apply {
        DefaultProtocolWebClientCustomizer().customize(it)
    }.build()

    private val mapper = ObjectMapper()

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
                    if (uri == "") {
                        logger.warn(marker, "unable to get metadata for $id: token URI is empty")
                        Mono.empty()
                    } else {
                        getByUri(uri)
                    }
                }
                .onErrorResume {
                    logger.warn("Unable to get properties for $id", it)
                    Mono.empty()
                }
        }
    }

    fun getByUri(uri: String): Mono<ItemProperties> {
        return client.get().uri(ipfsService.resolveRealUrl(uri))
            .retrieve()
            .bodyToMono<String>()
            .map {
                val node = mapper.readTree(it) as ObjectNode
                val attributes = node.path("attributes")
                logger.info("Got properties from $uri")

                ItemProperties(
                    name = node.getText("name", "label", "title") ?: "Untitled",
                    description = node.getText("description"),
                    image = node.getText("image", "image_url"),
                    imagePreview = null,
                    imageBig = null,
                    animationUrl = node.getText("animation_url"),
                    attributes = when {
                        attributes.isObject -> convertObjectAttributes(attributes.require())
                        attributes.isArray -> convertArrayAttributes(attributes.require())
                        else -> emptyList()
                    }
                )
            }
    }

    private fun getUri(token: Address, tokenId: BigInteger): Mono<String> {
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

    private fun getErc1155TokenUri(token: Address, tokenId: BigInteger): Mono<String> {
        return RaribleToken(token, sender).uri(tokenId)
    }

    private fun getErc721TokenUri(token: Address, tokenId: BigInteger): Mono<String> {
        return MintableToken(token, sender).tokenURI(tokenId)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PropertiesCacheDescriptor::class.java)
    }
}
