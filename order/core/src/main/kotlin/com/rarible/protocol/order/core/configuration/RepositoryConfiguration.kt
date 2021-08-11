package com.rarible.protocol.order.core.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.Package
import com.rarible.protocol.order.core.repository.currency.CurrencyRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.*
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.ConversionService
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@EnableMongoAuditing
@EnableRaribleMongo
@EnableScaletherMongoConversions
@EnableReactiveMongoRepositories(basePackageClasses = [Package::class])
class RepositoryConfiguration(
    private val template: ReactiveMongoTemplate,
    private val publisher: ProtocolOrderPublisher,
    private val conversionService: ConversionService
) {
    @Bean
    fun currencyRepository(): CurrencyRepository {
        return CurrencyRepository(template)
    }

    @Bean
    fun exchangeHistoryRepository(): ExchangeHistoryRepository {
        return ExchangeHistoryRepository(template)
    }

    @Bean
    fun orderRepository(): OrderRepository {
        val mongoOrderRepository = MongoOrderRepository(template)
        val optimizedSaveRepository = OptimizedOrderSaveRepositoryDecorator(mongoOrderRepository)
        return NotifiableOrderRepositoryDecorator(optimizedSaveRepository, publisher, conversionService)
    }

    @Bean
    fun orderVersionRepository(): OrderVersionRepository {
        return OrderVersionRepository(template)
    }

    @Bean
    fun openSeaFetchStateRepository(): OpenSeaFetchStateRepository {
        return OpenSeaFetchStateRepository(template)
    }
}
