package com.rarible.protocol.nft.api.configuration

import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.core.cache.EnableRaribleCache
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.core.loggingfilter.EnableLoggingContextFilter
import com.rarible.core.telemetry.actuator.WebRequestClientTagContributor
import com.rarible.ethereum.autoconfigure.EthereumProperties
import com.rarible.ethereum.nft.domain.EIP712DomainNftFactory
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.nft.core.configuration.PropertiesCore
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import scalether.transaction.MonoTransactionSender
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

@Configuration
@EnableConfigurationProperties(NftIndexerApiProperties::class)
@EnableRaribleCache
@EnableLoggingContextFilter
@EnableRaribleRedisLock
@EnableEthereumScanner
@PropertiesCore
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class NftIndexerApiConfiguration(
    private val nftIndexerApiProperties: NftIndexerApiProperties,
    private val ethereumProperties: EthereumProperties,
) {
    @Bean
    fun reduceSkipTokens(): ReduceSkipTokens {
        return ReduceSkipTokens.NO_SKIP_TOKENS
    }

    @Bean
    fun operatorProperties(): NftIndexerApiProperties.OperatorProperties {
        return nftIndexerApiProperties.operator
    }

    @Bean
    fun erc1271SignService(sender: MonoTransactionSender): ERC1271SignService {
        return ERC1271SignService(sender)
    }

    @Bean
    fun daonomicLazyNftValidator(
        sender: MonoTransactionSender,
        erc1271SignService: ERC1271SignService
    ): LazyNftValidator {
        return LazyNftValidator(
            erc1271SignService,
            EIP712DomainNftFactory(BigInteger.valueOf(nftIndexerApiProperties.chainId))
        )
    }

    @Bean
    fun webRequestClientTagContributor(): WebRequestClientTagContributor {
        return WebRequestClientTagContributor()
    }

    @Bean
    fun web3j(): Web3j {
        return Web3j.build(HttpService(ethereumProperties.httpUrl))
    }

    @Bean
    fun ensResolver(web3j: Web3j): EnsResolver {
        return EnsResolver(web3j)
    }
}
