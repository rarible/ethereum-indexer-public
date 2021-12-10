package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.convert
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import java.util.*

/**
 * Kafka consumer responsible for attaching metadata to NFT Collection events DTOs (consumed from an internal topic)
 * and forwarding the extended events to the public topic.
 *
 * This component is used in the 'nft-indexer-listener' module, which is running a background job (ConsumerWorker)
 * that is reading from the internal queue and publishes to the public queue.
 */
@Component
@CaptureSpan(SpanType.APP)
class InternalCollectionHandler(
    private val tokenMetaService: TokenMetaService,
    private val conversionService: ConversionService,
    private val protocolNftEventPublisher: ProtocolNftEventPublisher
) : ConsumerEventHandler<NftCollectionEventDto> {

    override suspend fun handle(event: NftCollectionEventDto) = when(event) {
        is NftCollectionUpdateEventDto -> {
            val meta = tokenMetaService.get(event.id)
            val metaDto = conversionService.convert<NftCollectionMetaDto>(meta)
            val extendedCollection = event.collection.copy(meta = metaDto)
            protocolNftEventPublisher.publish(event.copy(collection = extendedCollection))
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
