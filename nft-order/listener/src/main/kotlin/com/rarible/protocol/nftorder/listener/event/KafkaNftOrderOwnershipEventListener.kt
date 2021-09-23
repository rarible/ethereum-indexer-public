package com.rarible.protocol.nftorder.listener.event

import com.rarible.core.common.convert
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.NftOrderOwnershipEventDto
import com.rarible.protocol.dto.NftOrderOwnershipEventTopicProvider
import com.rarible.protocol.nftorder.core.event.OwnershipEvent
import com.rarible.protocol.nftorder.core.event.OwnershipEventListener
import com.rarible.protocol.nftorder.core.model.OwnershipId
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class KafkaNftOrderOwnershipEventListener(
    private val conversionService: ConversionService,
    private val eventsProducer: RaribleKafkaProducer<NftOrderOwnershipEventDto>
) : OwnershipEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val eventsHeaders = mapOf(
        "protocol.nft-order.event.version" to NftOrderOwnershipEventTopicProvider.VERSION
    )

    override suspend fun onEvent(event: OwnershipEvent) {

        val dto = conversionService.convert<NftOrderOwnershipEventDto>(event)
        val ownershipId = OwnershipId.parseId(dto.ownershipId)
        val itemId = "${ownershipId.token}:${ownershipId.tokenId.value}"

        val message = KafkaMessage(
            id = dto.eventId,
            key = itemId,
            value = dto,
            headers = eventsHeaders
        )
        eventsProducer.send(message).ensureSuccess()
        logger.info("Item Event sent: {}", dto)
    }
}
