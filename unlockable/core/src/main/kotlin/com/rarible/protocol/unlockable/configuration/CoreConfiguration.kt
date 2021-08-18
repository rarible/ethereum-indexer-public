package com.rarible.protocol.unlockable.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.unlockable.converter.LockDtoConverter
import com.rarible.protocol.unlockable.event.LockEventListener
import com.rarible.protocol.unlockable.repository.LockRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableRaribleMongo
@EnableScaletherMongoConversions
@EnableReactiveMongoRepositories(basePackageClasses = [LockRepository::class])
@EnableConfigurationProperties(LockEventProducerProperties::class)
@ComponentScan(
    basePackageClasses = [
        LockDtoConverter::class,
        LockEventListener::class,
        LockRepository::class
    ]
)
class CoreConfiguration {

    @Value("\${common.blockchain}")
    private lateinit var blockchain: Blockchain

    @Bean
    fun blockchain(): Blockchain {
        return blockchain
    }
}
