package com.rarible.protocol.nft.core.configuration

import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.contracts.collection.*
import com.rarible.protocol.nft.core.converters.ConvertersPackage
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.service.Package
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@Configuration
@EnableConfigurationProperties(NftIndexerProperties::class)
@Import(RepositoryConfiguration::class, ProducerConfiguration::class)
@ComponentScan(basePackageClasses = [Package::class, ConvertersPackage::class])
class CoreConfiguration {
    @Bean
    fun logEventService(mongo: ReactiveMongoOperations): LogEventService {
        val nftItemHistoryTopics = (ItemType.TRANSFER.topic + ItemType.ROYALTY.topic + ItemType.CREATORS.topic)
            .associateWith { NftItemHistoryRepository.COLLECTION }

        val nftHistoryTopics = listOf(
            CreateERC1155_v1Event.id(),
            CreateEvent.id(),
            CreateERC721_v4Event.id(),
            CreateERC721RaribleUserEvent.id(),
            CreateERC721RaribleEvent.id(),
            CreateERC1155RaribleUserEvent.id(),
            CreateERC1155RaribleEvent.id()
        ).associateWith {NftHistoryRepository.COLLECTION }

        return LogEventService(nftItemHistoryTopics + nftHistoryTopics, mongo)
    }
}