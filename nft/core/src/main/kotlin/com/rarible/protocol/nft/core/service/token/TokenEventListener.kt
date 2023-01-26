package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.nft.core.converters.dto.ExtendedCollectionDtoConverter
import com.rarible.protocol.nft.core.model.ExtendedToken
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.stereotype.Component
import java.util.*

@Component
class TokenEventListener(
    private val eventPublisher: ProtocolNftEventPublisher
) {

    suspend fun onTokenChanged(token: Token, event: TokenEvent? = null) {
        val updateEvent = NftCollectionUpdateEventDto(
            eventId = token.lastEventId ?: UUID.randomUUID().toString(),
            id = token.id,
            collection = ExtendedCollectionDtoConverter.convert(ExtendedToken(token, TokenMeta.EMPTY)),
            eventTimeMarks = blockchainEventMark(event?.log?.blockTimestamp)
        )
        eventPublisher.publishInternalCollection(updateEvent)
    }
}
