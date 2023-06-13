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
import com.rarible.protocol.order.api.service.order.validation.OrderStateValidator
import com.rarible.protocol.order.api.service.order.validation.validators.CheckingOrderStateValidator
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.OrderCancelService
import com.rarible.protocol.order.core.service.looksrare.LooksrareOrderService
import com.rarible.protocol.order.core.service.x2y2.X2Y2Service
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
    fun bidValidation(): OrderIndexerProperties.BidValidationProperties {
        return indexerProperties.bidValidation
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
    fun x2y2OrderStateValidator(
        x2Y2Service: X2Y2Service,
        orderCancelService: OrderCancelService,
    ): OrderStateValidator = CheckingOrderStateValidator(
        orderStateCheckService = x2Y2Service,
        orderCancelService = orderCancelService,
        platform = Platform.X2Y2,
    )

    @Bean
    fun looksrareOrderStateValidator(
        looksrareOrderService: LooksrareOrderService,
        orderCancelService: OrderCancelService,
    ): OrderStateValidator = CheckingOrderStateValidator(
        orderStateCheckService = looksrareOrderService,
        orderCancelService = orderCancelService,
        platform = Platform.LOOKSRARE,
    )
}
