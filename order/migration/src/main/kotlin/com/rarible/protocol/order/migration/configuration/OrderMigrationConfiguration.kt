package com.rarible.protocol.order.migration.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.migration.model.SpringDataMongodb
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

@EnableMongock
@Configuration
@EnableScaletherMongoConversions
@EnableRaribleMongo
class OrderMigrationConfiguration {

    @Bean
    fun springDataMongodb(@Value("\${spring.data.mongodb.uri}") mongodbUri: String): SpringDataMongodb {
        return SpringDataMongodb(mongodbUri)
    }

    @Bean
    fun blockchain(): Blockchain {
        return Blockchain.ETHEREUM
    }

    @Bean
    fun transactionManager(rdbf: ReactiveMongoDatabaseFactory) = ReactiveMongoTransactionManager(rdbf)

    @Bean
    fun transactionOperator(rtm: ReactiveTransactionManager) = TransactionalOperator.create(rtm)
}
