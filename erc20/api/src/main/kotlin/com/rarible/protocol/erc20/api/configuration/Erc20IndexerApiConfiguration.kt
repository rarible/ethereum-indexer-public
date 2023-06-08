package com.rarible.protocol.erc20.api.configuration

import com.rarible.core.loggingfilter.EnableLoggingContextFilter
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.core.telemetry.actuator.WebRequestClientTagContributor
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableRaribleMongo
@EnableContractService
@EnableLoggingContextFilter
@EnableScaletherMongoConversions
@EnableConfigurationProperties(Erc20IndexerApiProperties::class)
class Erc20IndexerApiConfiguration {

    @Bean
    fun webRequestClientTagContributor(): WebRequestClientTagContributor {
        return WebRequestClientTagContributor()
    }
}
