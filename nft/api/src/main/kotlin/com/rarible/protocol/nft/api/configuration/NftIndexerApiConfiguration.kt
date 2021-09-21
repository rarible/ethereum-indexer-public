package com.rarible.protocol.nft.api.configuration

import com.rarible.core.cache.EnableRaribleCache
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.core.loggingfilter.EnableLoggingContextFilter
import com.rarible.ethereum.nft.domain.EIP712DomainNftFactory
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.nft.core.configuration.IpfsProperties
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger

@Configuration
@EnableConfigurationProperties(NftIndexerApiProperties::class, IpfsProperties::class)
@EnableRaribleCache
@EnableLoggingContextFilter
@EnableRaribleRedisLock
@ComponentScan(basePackages = ["com.rarible.protocol.nft.api.converters"])
class NftIndexerApiConfiguration(
    private val nftIndexerApiProperties: NftIndexerApiProperties
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
    fun daonomicLazyNftValidator(transactionSender: MonoTransactionSender): LazyNftValidator {
        return LazyNftValidator(
            ERC1271SignService(transactionSender),
            EIP712DomainNftFactory(BigInteger.valueOf(nftIndexerApiProperties.chainId))
        )
    }
}
