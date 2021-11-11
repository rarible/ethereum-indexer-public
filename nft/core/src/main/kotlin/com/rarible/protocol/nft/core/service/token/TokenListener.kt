package com.rarible.protocol.nft.core.service.token

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.converters.dto.CollectionDtoConverter
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.span.SpanType
import org.springframework.stereotype.Component
import java.util.*

@Component
@CaptureSpan(type = SpanType.SERVICE, subtype = "token-listener")
class TokenListener(
    private val eventPublisher: ProtocolNftEventPublisher,
    private val collectionDtoConverter: CollectionDtoConverter
) {
    suspend fun onTokenChanged(token: Token) {
        val updateEvent = NftCollectionUpdateEventDto(
            eventId = token.lastEventId ?: UUID.randomUUID().toString(),
            id = token.id,
            collection = collectionDtoConverter.convert(token)
        )
        eventPublisher.publish(updateEvent)
    }
}
