package com.rarible.protocol.client

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.web.reactive.function.client.WebClient

class CompositeWebClientCustomizer(private val customizers: List<WebClientCustomizer>) : WebClientCustomizer {
    override fun customize(webClientBuilder: WebClient.Builder) {
        customizers.forEach { customizer -> customizer.customize(webClientBuilder) }
    }
}