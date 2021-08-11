package com.rarible.protocol.erc20.core.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.config.EnableMongoAuditing

@EnableRaribleMongo
@EnableScaletherMongoConversions
@EnableMongoAuditing
@ComponentScan(basePackageClasses = [com.rarible.protocol.erc20.core.repository.Package::class])
class RepositoryConfiguration
