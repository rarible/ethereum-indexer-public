package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import com.rarible.protocol.nft.core.service.item.meta.descriptors.UserAgentGenerator
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Client responsible for making HTTP requests to external APIs.
 * Handles OpenSea's API separately (by using a dedicated HTTP proxy).
 */
@Component
class ExternalHttpClient(
    @Value("\${api.opensea.url:}") val openseaUrl: String,
    @Value("\${api.opensea.api-key:}") private val openseaApiKey: String,
    @Value("\${api.opensea.read-timeout}") private val readTimeout: Int,
    @Value("\${api.opensea.connect-timeout}") private val connectTimeout: Int,
    @Value("\${api.proxy-url:}") private val proxyUrl: String
) {

    protected val defaultClient = WebClient.builder().apply {
        DefaultProtocolWebClientCustomizer().customize(it)
    }.build()

    protected val openSeaClient = WebClient.builder()
        .clientConnector(createConnector(connectTimeout, readTimeout, proxyUrl, true))
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs { it.defaultCodecs().maxInMemorySize(262144 * 5) }
            .build())
        .build()

    fun get(url: String): WebClient.ResponseSpec {
        val get = if (url.startsWith(openseaUrl)) {
            val openSeaGet = openSeaClient.get()
            if (openseaApiKey.isNotBlank()) {
                openSeaGet.header(X_API_KEY, openseaApiKey)
            }
            if (proxyUrl.isNotBlank()) {
                openSeaGet.header(HttpHeaders.USER_AGENT, UserAgentGenerator.generateUserAgent())
            }
            openSeaGet
        } else {
            defaultClient.get()
        }
        // May throw "invalid URL" exception.
        get.uri(url)
        return get.retrieve()
    }
}

private const val X_API_KEY = "X-API-KEY"
private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(60)

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
