package com.rarible.protocol.order.api.configuration

import com.rarible.core.loggingfilter.EnableLoggingContextFilter
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.core.telemetry.actuator.WebRequestClientTagContributor
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.nft.domain.EIP712DomainNftFactory
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.opensea.client.agent.UserAgentProvider
import com.rarible.protocol.order.api.service.order.validation.validators.LazyAssetValidator
import com.rarible.protocol.order.api.service.order.validation.validators.MinimalPriceItemBidValidator
import com.rarible.protocol.order.api.service.order.validation.validators.OrderDataValidator
import com.rarible.protocol.order.api.service.order.validation.validators.OrderSignatureValidator
import com.rarible.protocol.order.api.service.order.validation.validators.OrderStartEndDateValidator
import com.rarible.protocol.order.api.service.order.validation.validators.ParametersPatchValidator
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.metric.NoopOrderValidationMetrics
import com.rarible.protocol.order.core.validator.CompositeOrderValidator
import com.rarible.protocol.order.core.validator.OrderValidator
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
    private val indexerProperties: OrderIndexerProperties,
    private val apiProperties: OrderIndexerApiProperties
) {

    @Bean
    fun bidValidation(): OrderIndexerProperties.BidValidationProperties {
        return indexerProperties.bidValidation
    }

    @Bean
    fun blockchain(): Blockchain {
        return indexerProperties.blockchain
    }

    @Bean
    fun orderSettingsProperties() = apiProperties.settings

    @Bean
    fun daonomicLazyNftValidator(erc1271SignService: ERC1271SignService): LazyNftValidator {
        return LazyNftValidator(
            erc1271SignService,
            EIP712DomainNftFactory(BigInteger.valueOf(indexerProperties.chainId.toLong()))
        )
    }

    @Bean
    fun webRequestClientTagContributor(): WebRequestClientTagContributor {
        return WebRequestClientTagContributor()
    }

    @Bean
    fun openSeaDefaultUserAgentProvider(): UserAgentProvider {
        return object : UserAgentProvider {
            override fun get(): String {
                return "PostmanRuntime/7.31.1"
            }
        }
    }

    @Bean
    fun apiOrderValidator(
        lazyAssetValidator: LazyAssetValidator,
        minimalPriceItemBidValidator: MinimalPriceItemBidValidator,
        orderDataValidator: OrderDataValidator,
        orderSignatureValidator: OrderSignatureValidator,
        orderStartEndDateValidator: OrderStartEndDateValidator,
        parametersPatchValidator: ParametersPatchValidator,
        noopOrderValidationMetrics: NoopOrderValidationMetrics,
    ): OrderValidator =
        CompositeOrderValidator(
            type = "api_order_validator",
            validators = listOf(
                lazyAssetValidator,
                minimalPriceItemBidValidator,
                orderDataValidator,
                orderSignatureValidator,
                orderStartEndDateValidator,
                parametersPatchValidator,
            ),
            orderValidationMetrics = noopOrderValidationMetrics,
        )
}
