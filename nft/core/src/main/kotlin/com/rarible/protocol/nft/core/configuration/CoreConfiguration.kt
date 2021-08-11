package com.rarible.protocol.nft.core.configuration

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.contracts.collection.*
import com.rarible.protocol.contracts.creators.CreatorsEvent
import com.rarible.protocol.contracts.royalties.RoyaltiesSetEvent
import com.rarible.protocol.contracts.royalties.SecondarySaleFeesEvent
import com.rarible.protocol.nft.core.converters.ConvertersPackage
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
    fun logEventService(mongo: ReactiveMongoOperations) =
        LogEventService(
            mapOf(
                TransferBatchEvent.id() to NftItemHistoryRepository.COLLECTION,
                TransferSingleEvent.id() to NftItemHistoryRepository.COLLECTION,
                TransferEvent.id() to NftItemHistoryRepository.COLLECTION,
                SecondarySaleFeesEvent.id() to NftItemHistoryRepository.COLLECTION,
                RoyaltiesSetEvent.id() to NftItemHistoryRepository.COLLECTION,
                CreatorsEvent.id() to NftItemHistoryRepository.COLLECTION,
                CreateERC1155_v1Event.id() to NftHistoryRepository.COLLECTION,
                CreateEvent.id() to NftHistoryRepository.COLLECTION,
                CreateERC721_v4Event.id() to NftHistoryRepository.COLLECTION,
                CreateERC721RaribleUserEvent.id() to NftHistoryRepository.COLLECTION,
                CreateERC721RaribleEvent.id() to NftHistoryRepository.COLLECTION,
                CreateERC1155RaribleUserEvent.id() to NftHistoryRepository.COLLECTION,
                CreateERC1155RaribleEvent.id() to NftHistoryRepository.COLLECTION
            ),
            mongo
        )
}