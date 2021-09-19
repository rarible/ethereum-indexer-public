package com.rarible.protocol.nft.core.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemPropertyRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.time.Clock

@EnableMongoAuditing
@EnableScaletherMongoConversions
@EnableRaribleMongo
@EnableReactiveMongoRepositories(basePackageClasses = [com.rarible.protocol.nft.core.repository.Package::class])
@ComponentScan(basePackageClasses = [com.rarible.protocol.nft.core.repository.Package::class])
class RepositoryConfiguration(
    private val mongo: ReactiveMongoOperations
) {
    @Bean
    fun historyRepository(): NftItemHistoryRepository {
        return NftItemHistoryRepository(mongo)
    }

    @Bean
    fun lazyNftItemHistoryRepository(): LazyNftItemHistoryRepository {
        return LazyNftItemHistoryRepository(mongo)
    }

    @Bean
    fun itemRepository(): ItemRepository {
        return ItemRepository(mongo)
    }

    @Bean
    fun tokenRepository(): TokenRepository {
        return TokenRepository(mongo)
    }

    @Bean
    fun clock(): Clock {
        return Clock.systemUTC()
    }

    @Bean
    fun itemPropertyRepository(clock: Clock): ItemPropertyRepository {
        return ItemPropertyRepository(mongo, clock)
    }

    @Bean
    fun ownershipRepository(): OwnershipRepository {
        return OwnershipRepository(mongo)
    }

    @Bean
    fun tokenHistoryRepository(): NftHistoryRepository {
        return NftHistoryRepository(mongo)
    }

    @Bean
    fun tempTaskRepository(): TempTaskRepository {
        return TempTaskRepository(mongo)
    }
}
