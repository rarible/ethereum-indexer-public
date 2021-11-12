package com.rarible.protocol.order.api.configuration

import com.rarible.protocol.order.core.formatter.InstantFormatter
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class WebFluxConfiguration : WebFluxConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addFormatter(InstantFormatter())
    }

}