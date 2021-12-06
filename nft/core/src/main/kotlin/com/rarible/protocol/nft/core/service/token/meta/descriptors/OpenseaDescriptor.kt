package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.UserAgentGenerator
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getInt
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getText
import com.rarible.protocol.nft.core.service.token.meta.TokenMetaService
import io.netty.handler.timeout.ReadTimeoutHandler
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import scalether.domain.Address
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class OpenseaDescriptor(
    private val mapper: ObjectMapper,
    @Value("\${api.opensea.url:}") private val openseaUrl: String,
    @Value("\${api.opensea.api-key:}") private val openseaApiKey: String,
    @Value("\${api.opensea.read-timeout}") private val readTimeout: Int,
    @Value("\${api.opensea.connect-timeout}") private val connectTimeout: Int,
    @Value("\${api.opensea.request-timeout}") private val requestTimeout: Long,
    @Value("\${api.proxy-url:}") private val proxyUrl: String
): TokenPropertiesDescriptor {

    private val exchangeStrategies = ExchangeStrategies.builder()
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(262144 * 5)
        }
        .build()

    private val client = WebClient.builder()
        .clientConnector(createConnector(connectTimeout, readTimeout, proxyUrl, true))
        .exchangeStrategies(exchangeStrategies)
        .build()

    override suspend fun resolve(id: Address): TokenProperties? {
        if (openseaUrl.isBlank()) return null
        val httpUrl = "$openseaUrl/asset_contract/${id.prefixed()}"
        logger.info("OpenSea: getting properties from $httpUrl")
        val get = client.get().uri(httpUrl)
        if (openseaApiKey.isNotBlank()) {
            get.header(X_API_KEY, openseaApiKey)
        }
        if (proxyUrl.isNotBlank()) {
            get.header(HttpHeaders.USER_AGENT, UserAgentGenerator.generateUserAgent())
        }
        return get.retrieve()
            .bodyToMono<String>()
            .onErrorResume {
                logger.error(
                    "OpenSea: failed to get properties for URI: $httpUrl for token: $id" + if (it is WebClientResponseException) {
                        " response: ${it.rawStatusCode}: ${it.statusText}"
                    } else {
                        ""
                    }
                )
                Mono.empty()
            }
            .flatMap {
                TokenMetaService.logger.info("parsing properties by URI: $httpUrl for token: $id")
                mono { parseJsonProperties(it) }
            }
            .timeout(Duration.ofMillis(requestTimeout))
            .onErrorResume {
                TokenMetaService.logger.error("failed to parse properties by URI: $httpUrl for token: $id")
                Mono.empty()
            }
            .awaitFirstOrNull()
    }

    override fun order() = Int.MAX_VALUE


    private fun parseJsonProperties(jsonBody: String): TokenProperties? {
        val node = mapper.readTree(jsonBody) as ObjectNode
        return TokenProperties(
            name = node.getText("name") ?: "Untitled",
            description = node.getText("description"),
            image = node.getText("image_url"),
            external_link = node.getText("external_link"),
            seller_fee_basis_points = node.getInt("opensea_seller_fee_basis_points"),
            fee_recipient = node.getText("payout_address").let { Address.apply(it) } ?: null,
        )
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OpenseaDescriptor::class.java)
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
