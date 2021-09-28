package com.rarible.protocol.nftorder.core.configuration

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.dto.NftOrderItemEventTopicProvider
import com.rarible.protocol.dto.NftOrderOwnershipEventDto
import com.rarible.protocol.dto.NftOrderOwnershipEventTopicProvider
import com.rarible.protocol.nft.api.client.*
import com.rarible.protocol.nftorder.core.converter.OwnershipToDtoConverter
import com.rarible.protocol.nftorder.core.event.ItemEventListener
import com.rarible.protocol.nftorder.core.repository.ItemRepository
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import com.rarible.protocol.unlockable.api.client.UnlockableApiClientFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@EnableScaletherMongoConversions
@EnableRaribleMongo
@EnableReactiveMongoRepositories(basePackageClasses = [ItemRepository::class])
@EnableConfigurationProperties(EventProducerProperties::class)
@ComponentScan(
    basePackageClasses = [
        ItemService::class,
        OwnershipToDtoConverter::class,
        ItemRepository::class,
        ItemEventListener::class
    ]
)
class CoreConfiguration(
    private val eventProducerProperties: EventProducerProperties
) {

    @Value("\${common.blockchain}")
    private lateinit var blockchain: Blockchain

    @Bean
    fun blockchain(): Blockchain {
        return blockchain
    }

    @Bean
    fun nftItemControllerApi(nftIndexerApiClientFactory: NftIndexerApiClientFactory): NftItemControllerApi {
        return nftIndexerApiClientFactory.createNftItemApiClient(blockchain.value)
    }

    @Bean
    fun nftActivityControllerApi(nftIndexerApiClientFactory: NftIndexerApiClientFactory): NftActivityControllerApi {
        return nftIndexerApiClientFactory.createNftActivityApiClient(blockchain.value)
    }

    @Bean
    fun nftOwnershipControllerApi(nftIndexerApiClientFactory: NftIndexerApiClientFactory): NftOwnershipControllerApi {
        return nftIndexerApiClientFactory.createNftOwnershipApiClient(blockchain.value)
    }

    @Bean
    fun nftCollectionControllerApi(nftIndexerApiClientFactory: NftIndexerApiClientFactory): NftCollectionControllerApi {
        return nftIndexerApiClientFactory.createNftCollectionApiClient(blockchain.value)
    }

    @Bean
    fun nftLazyMintControllerApi(nftIndexerApiClientFactory: NftIndexerApiClientFactory): NftLazyMintControllerApi {
        return nftIndexerApiClientFactory.createNftMintApiClient(blockchain.value)
    }

    @Bean
    fun orderActivityControllerApi(orderIndexerApiClientFactory: OrderIndexerApiClientFactory): OrderActivityControllerApi {
        return orderIndexerApiClientFactory.createOrderActivityApiClient(blockchain.value)
    }

    @Bean
    fun orderControllerApi(orderIndexerApiClientFactory: OrderIndexerApiClientFactory): OrderControllerApi {
        return orderIndexerApiClientFactory.createOrderApiClient(blockchain.value)
    }

    @Bean
    fun lockControllerApi(unlockableApiClientFactory: UnlockableApiClientFactory): LockControllerApi {
        return unlockableApiClientFactory.createUnlockableApiClient(blockchain.value)
    }

    @Bean
    fun itemEventProducer(): RaribleKafkaProducer<NftOrderItemEventDto> {
        val env = eventProducerProperties.environment
        val blockchain = blockchain.value

        val clientId = "${env}.${blockchain}.protocol-nft-order-listener.item"

        return RaribleKafkaProducer(
            clientId = clientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftOrderItemEventDto::class.java,
            defaultTopic = NftOrderItemEventTopicProvider.getTopic(env, blockchain),
            bootstrapServers = eventProducerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun ownershipEventProducer(): RaribleKafkaProducer<NftOrderOwnershipEventDto> {
        val env = eventProducerProperties.environment
        val blockchain = blockchain.value

        val clientId = "${env}.${blockchain}.protocol-nft-order-listener.ownership"

        return RaribleKafkaProducer(
            clientId = clientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftOrderOwnershipEventDto::class.java,
            defaultTopic = NftOrderOwnershipEventTopicProvider.getTopic(env, blockchain),
            bootstrapServers = eventProducerProperties.kafkaReplicaSet
        )
    }

}