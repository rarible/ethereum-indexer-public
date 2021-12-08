package com.rarible.protocol.nft.core.configuration

import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.nft.core.converters.ConvertersPackage
import com.rarible.protocol.nft.core.model.CollectionEventType
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.service.Package
import org.bson.types.ObjectId
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.util.ClassUtils

@Configuration
@EnableConfigurationProperties(NftIndexerProperties::class)
@Import(RepositoryConfiguration::class, ProducerConfiguration::class)
@ComponentScan(basePackageClasses = [Package::class, ConvertersPackage::class])
class CoreConfiguration(
    private val properties: NftIndexerProperties
) {
    @Bean
    fun featureFlags(): NftIndexerProperties.FeatureFlags {
        return properties.featureFlags
    }

    @Bean
    fun logEventService(mongo: ReactiveMongoOperations): LogEventService {
        val nftItemHistoryTopics = (ItemType.TRANSFER.topic + ItemType.ROYALTY.topic + ItemType.CREATORS.topic)
            .associateWith { NftItemHistoryRepository.COLLECTION }

        val nftHistoryTopics = CollectionEventType.values().flatMap { it.topic }
            .associateWith { NftHistoryRepository.COLLECTION }

        return LogEventService(nftItemHistoryTopics + nftHistoryTopics, mongo)
    }

    @Bean
    fun mappingMongoConverter(
        context: MongoMappingContext,
        conversions: MongoCustomConversions
    ): MappingMongoConverter {
        val mappingConverter = object : MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context) {
            override fun convertId(id: Any, targetType: Class<*>?): Any? {
                if (id == null) {
                    return null
                    // prevent to convert string to objectId automatically
                } else if (ClassUtils.isAssignable(
                        ObjectId::class.java,
                        targetType
                    ) && id is String && ObjectId.isValid(id.toString())
                ) {
                    return id
                }
                return super.convertId(id, targetType)
            }
        }
        mappingConverter.setCustomConversions(conversions)
        return mappingConverter
    }
}
