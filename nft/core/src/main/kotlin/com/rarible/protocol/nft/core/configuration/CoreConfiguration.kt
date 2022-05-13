package com.rarible.protocol.nft.core.configuration

import com.rarible.ethereum.log.service.LogEventService
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.configuration.EnableRaribleCacheLoader
import com.rarible.protocol.nft.core.converters.ConvertersPackage
import com.rarible.protocol.nft.core.event.EventListenerPackage
import com.rarible.protocol.nft.core.model.CollectionEventType
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.HistoryTopics
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.service.CollectionFeatureProvider
import com.rarible.protocol.nft.core.service.Package
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaCacheLoader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@Configuration
@EnableRaribleCacheLoader
@EnableConfigurationProperties(NftIndexerProperties::class)
@Import(RepositoryConfiguration::class, ProducerConfiguration::class, MetricsCountersConfiguration::class)
@ComponentScan(basePackageClasses = [
    Package::class,
    ConvertersPackage::class,
    EventListenerPackage::class
])
class CoreConfiguration(
    private val properties: NftIndexerProperties
) {

    @Bean
    fun featureFlags(): FeatureFlags {
        return properties.featureFlags
    }

    @Bean
    fun ipfsProperties(): NftIndexerProperties.IpfsProperties {
        return properties.ipfs
    }

    @Bean
    @Qualifier("meta.cache.loader.service")
    fun metaCacheLoaderService(
        cacheLoaderServices: List<CacheLoaderService<*>>
    ): CacheLoaderService<ItemMeta> =
        @Suppress(
            "UNCHECKED_CAST"
        ) (cacheLoaderServices.find { it.type == ItemMetaCacheLoader.TYPE } as CacheLoaderService<ItemMeta>)

    @Bean
    fun historyTopics(): HistoryTopics {
        val nftItemHistoryTopics = (ItemType.TRANSFER.topic + ItemType.ROYALTY.topic + ItemType.CREATORS.topic)
            .associateWith { NftItemHistoryRepository.COLLECTION }

        val nftHistoryTopics = CollectionEventType.values().flatMap { it.topic }
            .associateWith { NftHistoryRepository.COLLECTION }

        return HistoryTopics(nftItemHistoryTopics + nftHistoryTopics)
    }

    @Bean
    fun logEventService(mongo: ReactiveMongoOperations, historyTopics: HistoryTopics): LogEventService {
        return LogEventService(historyTopics, mongo)
    }

    @Bean
    fun contractAddresses(): NftIndexerProperties.ContractAddresses {
        return properties.contractAddresses
    }

    @Bean
    fun CollectionFeatureProvider(): CollectionFeatureProvider {
        return CollectionFeatureProvider(properties.blockchain)
    }
}
