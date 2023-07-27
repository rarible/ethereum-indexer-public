package com.rarible.protocol.erc20.migration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableMongock
@EnableScaletherMongoConversions
@EnableRaribleMongo
@SpringBootApplication
class Erc20MigrationApplication

fun main(args: Array<String>) {
    runApplication<Erc20MigrationApplication>(*args)
}
