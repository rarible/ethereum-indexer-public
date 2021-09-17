package com.rarible.protocol.order.api.configuration

import com.rarible.core.loggingfilter.EnableLoggingContextFilter
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.nft.domain.EIP712DomainNftFactory
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.FeatureFlags
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigInteger

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
    fun featureFlags(): FeatureFlags {
        return indexerProperties.featureFlags
    }

    @Bean
    fun blockchain(): Blockchain {
        return indexerProperties.blockchain
    }

    @Bean
    fun daonomicLazyNftValidator(erc1271SignService: ERC1271SignService): LazyNftValidator {
        return LazyNftValidator(
            erc1271SignService,
            EIP712DomainNftFactory(BigInteger.valueOf(indexerProperties.chainId.toLong()))
        )
    }

}
