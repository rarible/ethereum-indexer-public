package com.rarible.protocol.order.core.configuration

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigInteger

@Configuration
@EnableConfigurationProperties(OrderIndexerProperties::class)
class OrderIndexerPropertiesConfiguration(
    private val indexerProperties: OrderIndexerProperties
) {
    @Bean
    fun exchangeContractAddresses() = indexerProperties.exchangeContractAddresses

    @Bean
    fun auctionContractAddresses() = indexerProperties.auctionContractAddresses

    @Bean
    fun transferProxyAddresses() = indexerProperties.transferProxyAddresses

    @Bean
    fun currencyContractAddresses() = indexerProperties.currencyContractAddresses

    @Bean
    fun raribleOrderExpiration() = indexerProperties.raribleOrderExpiration

    @Bean
    @Qualifier("raribleExchangeV2")
    fun raribleEip712Domain(): EIP712Domain {
        logger.info("Order-Api was started with chainId=${indexerProperties.chainId}")
        return with(indexerProperties) {
            EIP712Domain(
                eip712DomainName,
                eip712DomainVersion,
                BigInteger.valueOf(chainId.toLong()),
                exchangeContractAddresses.v2
            )
        }
    }

    @Bean
    @Qualifier("openseaExchange")
    fun openseaEip712Domain(): EIP712Domain {
        return with(indexerProperties) {
            EIP712Domain(
                indexerProperties.openseaEip712DomainName,
                indexerProperties.openseaEip712DomainVersion,
                BigInteger.valueOf(chainId.toLong()),
                exchangeContractAddresses.openSeaV2
            )
        }
    }

    @Bean
    fun protocolCommissionProvider(): ProtocolCommissionProvider {
       return ProtocolCommissionProvider(EthUInt256.of(indexerProperties.protocolCommission.toLong()))
    }

    private companion object {
        val logger = LoggerFactory.getLogger(OrderIndexerPropertiesConfiguration::class.java)
    }
}
