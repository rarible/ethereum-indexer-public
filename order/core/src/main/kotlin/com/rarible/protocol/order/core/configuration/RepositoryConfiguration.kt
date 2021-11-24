package com.rarible.protocol.order.core.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.order.core.repository.Package
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.config.EnableMongoAuditing

@EnableMongoAuditing
@EnableRaribleMongo
@EnableScaletherMongoConversions
@ComponentScan(basePackageClasses = [Package::class])
class RepositoryConfiguration
