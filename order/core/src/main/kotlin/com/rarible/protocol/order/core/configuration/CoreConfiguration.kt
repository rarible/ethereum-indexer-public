package com.rarible.protocol.order.core.configuration

import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.StringToAddressConverter
import com.rarible.ethereum.converters.StringToBinaryConverter
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.contracts.exchange.wyvern.OrdersMatchedEvent
import com.rarible.protocol.contracts.exchange.v1.BuyEvent
import com.rarible.protocol.contracts.exchange.v1.CancelEvent
import com.rarible.protocol.contracts.exchange.v2.events.CancelEventDeprecated
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEventDeprecated
import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.order.core.converters.ConvertersPackage
import com.rarible.protocol.order.core.event.EventPackage
import com.rarible.protocol.order.core.model.FeatureFlags
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.Package
import com.rarible.protocol.order.core.trace.TracePackage
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import scalether.transaction.MonoTransactionSender

@Configuration
@EnableContractService
@ComponentScan(basePackageClasses = [
    Package::class,
    ConvertersPackage::class,
    EventPackage::class,
    TracePackage::class]
)
@Import(
    RepositoryConfiguration::class,
    ProducerConfiguration::class,
    ApiClientConfiguration::class,
    OrderIndexerPropertiesConfiguration::class
)
class CoreConfiguration {

    @Bean
    @ConfigurationPropertiesBinding
    fun stringToAddressConverter() = StringToAddressConverter()

    @Bean
    @ConfigurationPropertiesBinding
    fun stringToBinaryConverter() = StringToBinaryConverter()

    @Bean
    fun erc1271SignService(sender: MonoTransactionSender): ERC1271SignService {
        return ERC1271SignService(sender)
    }

    @Bean
    fun logEventService(mongo: ReactiveMongoOperations) =
        LogEventService(
            mapOf(
                BuyEvent.id() to ExchangeHistoryRepository.COLLECTION,
                CancelEvent.id() to ExchangeHistoryRepository.COLLECTION,
                MatchEventDeprecated.id() to ExchangeHistoryRepository.COLLECTION,
                MatchEvent.id() to ExchangeHistoryRepository.COLLECTION,
                com.rarible.protocol.contracts.exchange.v2.events.CancelEvent.id() to ExchangeHistoryRepository.COLLECTION,
                CancelEventDeprecated.id() to ExchangeHistoryRepository.COLLECTION,
                OrdersMatchedEvent.id() to ExchangeHistoryRepository.COLLECTION,
                OrderCancelledEvent.id() to ExchangeHistoryRepository.COLLECTION
            ),
            mongo
        )
}
