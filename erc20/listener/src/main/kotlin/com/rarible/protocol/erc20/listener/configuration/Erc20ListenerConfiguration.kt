package com.rarible.protocol.erc20.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.reduce.blockchain.BlockchainSnapshotStrategy
import com.rarible.core.reduce.service.ReduceService
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.protocol.erc20.core.repository.BalanceSnapshotRepository
import com.rarible.protocol.erc20.core.configuration.ProducerConfiguration
import com.rarible.protocol.erc20.core.listener.KafkaErc20BalanceEventListener
import com.rarible.protocol.erc20.core.producer.ProtocolEventPublisher
import com.rarible.protocol.erc20.listener.Erc20ListenerApplication
import com.rarible.protocol.erc20.listener.service.balance.BalanceReducer
import com.rarible.protocol.erc20.listener.service.balance.Erc20BalanceUpdateService
import com.rarible.protocol.erc20.listener.service.balance.Erc20BalanceReduceEventRepository
import com.rarible.protocol.erc20.listener.service.balance.Erc20BalanceReduceService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.convert.ConversionService
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender

@EnableMongock
@Configuration
@EnableContractService
@EnableScaletherMongoConversions
@EnableLogListeners(scanPackage = [Erc20ListenerApplication::class])
@EnableConfigurationProperties(Erc20ListenerProperties::class)
@Import(ProducerConfiguration::class)
class Erc20ListenerConfiguration {

    @Bean
    fun sender(ethereum: MonoEthereum): ReadOnlyMonoTransactionSender {
        return ReadOnlyMonoTransactionSender(ethereum, Address.ZERO())
    }

    @Bean
    fun reduceService(
        balanceReducer: BalanceReducer,
        eventRepository: Erc20BalanceReduceEventRepository,
        snapshotRepository: BalanceSnapshotRepository,
        erc20BalanceUpdateService: Erc20BalanceUpdateService,
        properties: Erc20ListenerProperties
    ): Erc20BalanceReduceService {
        return ReduceService(
            reducer = balanceReducer,
            eventRepository = eventRepository,
            snapshotRepository = snapshotRepository,
            updateService = erc20BalanceUpdateService,
            snapshotStrategy = BlockchainSnapshotStrategy(properties.blockCountBeforeSnapshot)
        )
    }

    @Bean
    fun kafkaErc20BalanceEventListener(
        conversionService: ConversionService,
        protocolEventPublisher: ProtocolEventPublisher
    ): KafkaErc20BalanceEventListener {
        return KafkaErc20BalanceEventListener(conversionService, protocolEventPublisher)
    }
}
