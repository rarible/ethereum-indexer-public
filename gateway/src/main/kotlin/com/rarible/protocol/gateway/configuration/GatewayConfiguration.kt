package com.rarible.protocol.gateway.configuration

import com.rarible.core.autoconfigure.filter.cors.EnableRaribleCorsWebFilter
import com.rarible.core.autoconfigure.nginx.EnableRaribleNginxExpose
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableRaribleCorsWebFilter
@EnableRaribleNginxExpose
@EnableConfigurationProperties(GatewayProperties::class)
class GatewayConfiguration {

    @Bean
    fun corsFilter(corsConfigurationSource: CorsConfigurationSource): CorsWebFilter {
        return CorsWebFilter(corsConfigurationSource)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration().applyPermitDefaultValues()
        config.addAllowedMethod(HttpMethod.GET)
        config.addAllowedMethod(HttpMethod.POST)
        config.maxAge = 3600
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
