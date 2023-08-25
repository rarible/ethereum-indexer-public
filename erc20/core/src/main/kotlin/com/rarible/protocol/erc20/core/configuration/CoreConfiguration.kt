package com.rarible.protocol.erc20.core.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.core.spring.YamlPropertySourceFactory
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.erc20.core.admin.AdminPackage
import com.rarible.protocol.erc20.core.converters.Erc20BalanceDtoConverter
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender

@EnableRaribleMongo
@EnableScaletherMongoConversions
@EnableMongoAuditing
@EnableReactiveMongoRepositories(basePackageClasses = [Erc20BalanceRepository::class])
@EnableConfigurationProperties(Erc20IndexerProperties::class)
@ComponentScan(
    basePackageClasses = [
        AdminPackage::class,
        Erc20BalanceService::class,
        Erc20BalanceRepository::class,
        Erc20BalanceDtoConverter::class
    ]
)
@PropertySource(
    value = ["classpath:config/core.yml"],
    factory = YamlPropertySourceFactory::class
)
class CoreConfiguration {
    @Bean
    fun sender(ethereum: MonoEthereum) = ReadOnlyMonoTransactionSender(ethereum, Address.ZERO())
}
