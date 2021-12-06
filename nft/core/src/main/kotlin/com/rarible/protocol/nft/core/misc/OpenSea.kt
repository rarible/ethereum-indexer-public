package com.rarible.protocol.nft.core.misc

import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit

val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(60)
const val X_API_KEY = "X-API-KEY"

fun createConnector(
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
