package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Deprecated("remove after release")
class InternalCollectionHandler(
    private val protocolNftEventPublisher: ProtocolNftEventPublisher
) : ConsumerEventHandler<NftCollectionEventDto> {

    override suspend fun handle(event: NftCollectionEventDto) = when(event) {
        is NftCollectionUpdateEventDto -> {
            protocolNftEventPublisher.publish(event)
        }
    }

    companion object {
        fun getInternalTopic(environment: String, blockchain: String): String =
            "protocol.$environment.$blockchain.indexer.nft.collection.internal"

        fun createInternalCollectionConsumer(
            applicationEnvironmentInfo: ApplicationEnvironmentInfo,
            blockchain: Blockchain,
            kafkaReplicaSet: String
        ): RaribleKafkaConsumer<NftCollectionEventDto> {
            val environment = applicationEnvironmentInfo.name
            val host = applicationEnvironmentInfo.host
            val consumerGroup = "$environment.protocol.${blockchain.value}.nft.indexer.collection.internal"
            val clientIdPrefix = "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"
            return RaribleKafkaConsumer(
                clientId = "$clientIdPrefix.nft.indexer.collection.internal",
                valueDeserializerClass = JsonDeserializer::class.java,
                valueClass = NftCollectionEventDto::class.java,
                consumerGroup = consumerGroup,
                defaultTopic = getInternalTopic(environment, blockchain.value),
                bootstrapServers = kafkaReplicaSet
            )
        }
    }
}
