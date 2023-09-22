package com.rarible.protocol.order.core.misc

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.util.unit.DataSize
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

object WebClientFactory {

    private val maxBodySize = DataSize.ofMegabytes(10).toBytes().toInt()
    private val timeout: Duration = Duration.ofSeconds(30)

    fun createClient(baseUrl: String): WebClient {
        val mapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val httpClient = HttpClient.create(ConnectionProvider.newConnection())
            .responseTimeout(timeout)
        val strategies = ExchangeStrategies
            .builder()
            .codecs { configurer: ClientCodecConfigurer ->
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON))
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON))
            }.build()
        val webClient = WebClient.builder()
            .exchangeStrategies(strategies)
            .clientConnector(ReactorClientHttpConnector(httpClient))
        webClient.codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(maxBodySize)
        }
        return webClient.baseUrl(baseUrl).build()
    }
}
