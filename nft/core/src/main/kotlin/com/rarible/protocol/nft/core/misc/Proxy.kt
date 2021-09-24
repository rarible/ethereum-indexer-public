package com.rarible.protocol.nft.core.misc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.tcp.ProxyProvider
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit

object Proxy {
    fun createConnector(
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        proxyUrl: String,
        followRedirect: Boolean
    ): ClientHttpConnector {
        logger.info("createConnector $connectTimeoutMs $readTimeoutMs $proxyUrl $followRedirect")

        val provider = ConnectionProvider.builder("protocol-default-open_sea-connection-provider")
            .maxConnections(200)
            .pendingAcquireMaxCount(-1)
            .maxIdleTime(DEFAULT_TIMEOUT)
            .maxLifeTime(DEFAULT_TIMEOUT)
            .lifo()
            .build()

        val tcpClient = reactor.netty.tcp.TcpClient.create(provider)
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .doOnConnected { conn: reactor.netty.Connection ->
                conn.addHandlerLast(
                    io.netty.handler.timeout.ReadTimeoutHandler(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                )
            }

        if (proxyUrl.isNotBlank()) {
            val finalTcpClient = tcpClient.proxy { provider ->
                val uri = URI.create(proxyUrl)
                val user = uri.userInfo?.split(":")
                user?.let { provider.type(ProxyProvider.Proxy.HTTP)
                    .host(uri.host).port(uri.port)
                    .username(user[0]).password { user[1] }
                } ?: provider.type(ProxyProvider.Proxy.HTTP).host(uri.host).port(uri.port)
            }
            return ReactorClientHttpConnector(
                HttpClient.from(finalTcpClient).followRedirect(followRedirect)
            )
        }

        return ReactorClientHttpConnector(
            HttpClient.from(tcpClient).followRedirect(followRedirect)
        )
    }

    private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(60)
    private val logger: Logger = LoggerFactory.getLogger(Proxy::class.java)
}
