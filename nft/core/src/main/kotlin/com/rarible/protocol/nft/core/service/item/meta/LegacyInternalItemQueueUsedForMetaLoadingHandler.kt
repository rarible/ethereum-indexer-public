package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.model.ItemId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Deprecated("Remove after we deploy meta 2.0")
@Component
class LegacyInternalItemQueueUsedForMetaLoadingHandler(
    private val itemMetaService: ItemMetaService
) : ConsumerEventHandler<NftItemEventDto> {

    private val logger = LoggerFactory.getLogger(LegacyInternalItemQueueUsedForMetaLoadingHandler::class.java)

    override suspend fun handle(event: NftItemEventDto) {
        logger.info("Handle internal item event $event")
        when (event) {
            is NftItemUpdateEventDto -> itemMetaService.scheduleMetaUpdate(ItemId.parseId(event.item.id))
            is NftItemDeleteEventDto -> Unit
        }
    }

    companion object {
        private fun getInternalTopic(environment: String, blockchain: String): String =
            "protocol.$environment.$blockchain.indexer.nft.item.internal"

        fun createInternalItemConsumer(
            applicationEnvironmentInfo: ApplicationEnvironmentInfo,
            blockchain: Blockchain,
            kafkaReplicaSet: String
        ): RaribleKafkaConsumer<NftItemEventDto> {
            val environment = applicationEnvironmentInfo.name
            val host = applicationEnvironmentInfo.host
            val consumerGroup = "$environment.protocol.${blockchain.value}.nft.indexer.item.internal"
            val clientIdPrefix = "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"
            return RaribleKafkaConsumer(
                clientId = "$clientIdPrefix.nft.indexer.item.internal",
                valueDeserializerClass = JsonDeserializer::class.java,
                valueClass = NftItemEventDto::class.java,
                consumerGroup = consumerGroup,
                defaultTopic = getInternalTopic(environment, blockchain.value),
                bootstrapServers = kafkaReplicaSet
            )
        }
    }
}
