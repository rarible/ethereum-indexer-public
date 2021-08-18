package com.rarible.protocol.unlockable.api.configuration

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.UnlockableEventDto
import com.rarible.protocol.dto.UnlockableTopicProvider
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.unlockable.configuration.LockEventProducerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiConfiguration(
    private val nftIndexerApiClientFactory: NftIndexerApiClientFactory,
    private val properties: LockEventProducerProperties,
    private val blockchain: Blockchain
) {

    @Bean
    fun lockEventProducer(): RaribleKafkaProducer<UnlockableEventDto> {
        val env = properties.environment
        val blockchain = blockchain.value

        val clientId = "${properties.environment}.${blockchain}.protocol-unlockable.lock"

        return RaribleKafkaProducer(
            clientId = clientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = UnlockableEventDto::class.java,
            defaultTopic = UnlockableTopicProvider.getTopic(env, blockchain),
            bootstrapServers = properties.kafkaReplicaSet
        )
    }

    @Bean
    fun nftItemControllerApi(): NftItemControllerApi {
        return nftIndexerApiClientFactory.createNftItemApiClient(blockchain.value)
    }

    @Bean
    fun nftOwnershipControllerApi(): NftOwnershipControllerApi {
        return nftIndexerApiClientFactory.createNftOwnershipApiClient(blockchain.value)
    }

}