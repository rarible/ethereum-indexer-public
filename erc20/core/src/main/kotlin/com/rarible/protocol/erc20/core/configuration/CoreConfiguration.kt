package com.rarible.protocol.erc20.core.configuration

import com.rarible.blockchain.scanner.monitoring.BlockchainMonitor
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.client.monitoring.MonitoringCallback
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.erc20.core.admin.AdminPackage
import com.rarible.protocol.erc20.core.converters.Erc20BalanceDtoConverter
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import reactor.core.publisher.Mono
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
class CoreConfiguration(
    private val properties: Erc20IndexerProperties,
    private val meterRegistry: MeterRegistry
) {

    @Bean
    fun sender(ethereum: MonoEthereum) = ReadOnlyMonoTransactionSender(ethereum, Address.ZERO())

    @Bean
    @ConditionalOnMissingBean(MonitoringCallback::class)
    fun ethereumMonitoringCallback(): MonitoringCallback {
        val monitor = BlockchainMonitor(meterRegistry)
        return object : MonitoringCallback {
            override fun <T> onBlockchainCall(method: String, monoCall: () -> Mono<T>) =
                monitor.onBlockchainCall(properties.blockchain.value, method, monoCall)
        }
    }
}
