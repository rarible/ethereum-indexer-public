package com.rarible.protocol.order.core.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.order.core.repository.Package
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.core.repository.auction.AuctionSnapshotRepository
import com.rarible.protocol.order.core.repository.currency.CurrencyRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.*
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@EnableMongoAuditing
@EnableRaribleMongo
@EnableScaletherMongoConversions
@EnableReactiveMongoRepositories(basePackageClasses = [Package::class])
class RepositoryConfiguration(
    private val template: ReactiveMongoTemplate
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
        return MongoOrderRepository(template)
    }

    @Bean
    fun orderVersionRepository(): OrderVersionRepository {
        return OrderVersionRepository(template)
    }

    @Bean
    fun openSeaFetchStateRepository(): OpenSeaFetchStateRepository {
        return OpenSeaFetchStateRepository(template)
    }

    @Bean
    fun auctionRepository(): AuctionRepository {
        return AuctionRepository(template)
    }

    @Bean
    fun auctionHistoryRepository(): AuctionHistoryRepository {
        return AuctionHistoryRepository(template)
    }

    @Bean
    fun auctionSnapshotRepository(): AuctionSnapshotRepository {
        return AuctionSnapshotRepository(template)
    }
}
