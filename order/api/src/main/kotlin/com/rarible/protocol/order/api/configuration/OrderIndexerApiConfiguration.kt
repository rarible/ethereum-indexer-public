package com.rarible.protocol.order.api.configuration

import com.rarible.core.loggingfilter.EnableLoggingContextFilter
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableScaletherMongoConversions
@EnableContractService
@EnableRaribleMongo
@EnableLoggingContextFilter
@EnableConfigurationProperties(OrderIndexerProperties::class, OrderIndexerApiProperties::class)
class OrderIndexerApiConfiguration(
    private val indexerProperties: OrderIndexerProperties
) {
    @Bean
    fun blockchain(): Blockchain {
        return indexerProperties.blockchain
    }
}
