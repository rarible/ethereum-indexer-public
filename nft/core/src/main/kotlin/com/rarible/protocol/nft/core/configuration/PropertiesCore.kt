package com.rarible.protocol.nft.core.configuration

import com.rarible.core.spring.YamlPropertySourceFactory
import org.springframework.context.annotation.PropertySource

@PropertySource(
    value = [
        "classpath:config/application-core.yml",
        "classpath:config/application-core-\${common.blockchain}.yml",
    ],
    factory = YamlPropertySourceFactory::class
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertiesCore
