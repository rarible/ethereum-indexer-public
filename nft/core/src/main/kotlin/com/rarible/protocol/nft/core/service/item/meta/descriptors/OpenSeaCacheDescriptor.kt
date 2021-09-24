package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.nft.core.misc.Proxy
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.parseTokenId
import com.rarible.protocol.nft.core.service.item.meta.toProperties
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.math.BigInteger
import java.time.Duration

@Service
class OpenSeaCacheDescriptor(
    @Value("\${api.opensea.url:}") private val openseaUrl: String,
    @Value("\${api.opensea.api-key:}") private val openseaApiKey: String,
    @Value("\${api.opensea.read-timeout}") private val readTimeout: Int,
    @Value("\${api.opensea.connect-timeout}") private val connectTimeout: Int,
    @Value("\${api.opensea.cache-timeout}") private val cacheTimeout: Long,
    @Value("\${api.opensea.request-timeout}") private val requestTimeout: Long,
    @Value("\${api.proxy-url:}") private val proxyUrl: String,
    @Autowired(required = false) private val cacheService: CacheService?
) : CacheDescriptor<ItemProperties> {

    override val collection: String = "cache_opensea"
    override fun getMaxAge(value: ItemProperties?): Long =
        if (value == null || !value.correct) {
            DateUtils.MILLIS_PER_MINUTE * 5
        } else {
            cacheTimeout
        }

    private val exchangeStrategies = ExchangeStrategies.builder()
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(262144 * 5)
        }
        .build()

    private val client = WebClient.builder()
        .clientConnector(Proxy.createConnector(connectTimeout, readTimeout, proxyUrl, true))
        .exchangeStrategies(exchangeStrategies)
        .build()

    fun fetchAsset(token: Address, tokenId: BigInteger): Mono<ItemProperties> {
        if (openseaUrl.isBlank()) {
            return Mono.empty()
        }
        return cacheService.get("$token:$tokenId", this, true)
            .filter { it.correct }
    }

    fun resetAsset(token: Address, tokenId: BigInteger): Mono<Void> =
        cacheService?.reset("$token:$tokenId", this) ?: Mono.empty()

    override fun get(id: String): Mono<ItemProperties> {
        return LoggingUtils.withMarker { marker ->
            logger.info(marker, "get properties $id")
            val (token, tokenId) = id.parseTokenId()
            val get = client.get()
                .uri("$openseaUrl/asset/$token/$tokenId/")
            if (openseaApiKey.isNotBlank()) {
                get.header(X_API_KEY, openseaApiKey)
            }
            get.retrieve()
                .bodyToMono<ObjectNode>()
                .map {
                    ItemProperties(
                        name = name(it, tokenId),
                        description = it.getText("description"),
                        image = it.getText("image_original_url") ?: it.getText("image_url"),
                        imagePreview = it.getText("image_preview_url"),
                        imageBig = it.getText("image_url"),
                        animationUrl = it.getText("animation_url"),
                        attributes = it.path("traits").toProperties()
                    )
                }
                .timeout(Duration.ofMillis(requestTimeout))
                .onErrorResume {
                    if (it is WebClientResponseException) {
                        logger.warn(marker, "Unable to fetch asset using opensea $token:$tokenId status: ${it.statusCode}")
                    } else {
                        logger.warn(marker, "Unable to fetch asset using opensea $token:$tokenId", it)
                    }
                    Mono.empty()
                }
        }

    }

    private fun name(node: ObjectNode, tokenId: BigInteger): String {
        return node.getText("name")
            ?: node.get("asset_contract")?.getText("name")?.let { "$it #$tokenId" }
            ?: "#$tokenId"
    }

    companion object {
        const val X_API_KEY = "X-API-KEY"
        val logger: Logger = LoggerFactory.getLogger(OpenSeaCacheDescriptor::class.java)
    }
}

private val ItemProperties.correct: Boolean
    get() = this.name.isNotEmpty()
        && this.imagePreview.isNullOrBlank().not()
        && this.imageBig.isNullOrBlank().not()
