package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService.Companion.logProperties
import io.netty.handler.timeout.ReadTimeoutHandler
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import java.math.BigInteger
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
@CaptureSpan(type = META_CAPTURE_SPAN_TYPE)
class OpenSeaPropertiesResolver(
    @Value("\${api.opensea.url:}") private val openseaUrl: String,
    @Value("\${api.opensea.api-key:}") private val openseaApiKey: String,
    @Value("\${api.opensea.read-timeout}") private val readTimeout: Int,
    @Value("\${api.opensea.connect-timeout}") private val connectTimeout: Int,
    @Value("\${api.opensea.request-timeout}") private val requestTimeout: Long,
    @Value("\${api.proxy-url:}") private val proxyUrl: String
) : ItemPropertiesResolver {

    private val exchangeStrategies = ExchangeStrategies.builder()
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(262144 * 5)
        }
        .build()

    private val client = WebClient.builder()
        .clientConnector(createConnector(connectTimeout, readTimeout, proxyUrl, true))
        .exchangeStrategies(exchangeStrategies)
        .build()

    override val name get() = "OpenSea"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (openseaUrl.isBlank()) return null
        val openSeaUrl = "$openseaUrl/asset/${itemId.token}/${itemId.tokenId.value}/"
        logProperties(itemId, "OpenSea: getting properties from $openSeaUrl")
        val get = client.get().uri(openSeaUrl)
        if (openseaApiKey.isNotBlank()) {
            get.header(X_API_KEY, openseaApiKey)
        }
        if (proxyUrl.isNotBlank()) {
            get.header(HttpHeaders.USER_AGENT, UserAgentGenerator.generateUserAgent())
        }
        return get.retrieve()
            .bodyToMono<ObjectNode>()
            .map {
                val image = it.getText("image_original_url") ?: it.getText("image_url")
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
                logProperties(
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

    private fun parseName(node: ObjectNode, tokenId: BigInteger): String {
        return node.getText("name")
            ?: node.get("asset_contract")?.getText("name")?.let { "$it #$tokenId" }
            ?: "#$tokenId"
    }

    companion object {
        private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(60)
        private const val X_API_KEY = "X-API-KEY"

        private fun createConnector(
            connectTimeoutMs: Int,
            readTimeoutMs: Int,
            proxyUrl: String,
            @Suppress("SameParameterValue") followRedirect: Boolean
        ): ClientHttpConnector {
            val provider = ConnectionProvider.builder("protocol-default-open_sea-connection-provider")
                .maxConnections(200)
                .pendingAcquireMaxCount(-1)
                .maxIdleTime(DEFAULT_TIMEOUT)
                .maxLifeTime(DEFAULT_TIMEOUT)
                .lifo()
                .build()

            val tcpClient = reactor.netty.tcp.TcpClient.create(provider)
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .doOnConnected {
                    it.addHandlerLast(ReadTimeoutHandler(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS))
                }

            if (proxyUrl.isNotBlank()) {
                val finalTcpClient = tcpClient.proxy {
                    val uri = URI.create(proxyUrl)
                    val user = uri.userInfo.split(":")
                    it.type(ProxyProvider.Proxy.HTTP)
                        .host(uri.host)
                        .username(user[0])
                        .password { user[1] }
                        .port(uri.port)
                }
                return ReactorClientHttpConnector(
                    HttpClient.from(finalTcpClient).followRedirect(followRedirect)
                )
            }

            return ReactorClientHttpConnector(
                HttpClient.from(tcpClient).followRedirect(followRedirect)
            )
        }
    }
}
