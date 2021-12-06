package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.converters.dto.ExtendedCollectionDtoConverter
import com.rarible.protocol.nft.core.model.ExtendedToken
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.stereotype.Component
import java.util.*

@Component
class TokenEventListener(
    private val eventPublisher: ProtocolNftEventPublisher,
    private val collectionDtoConverter: ExtendedCollectionDtoConverter
) {
    suspend fun onTokenChanged(token: Token) {
        val updateEvent = NftCollectionUpdateEventDto(
            eventId = token.lastEventId ?: UUID.randomUUID().toString(),
            id = token.id,
            collection = collectionDtoConverter.convert(ExtendedToken(token, TokenMeta.EMPTY))
        )
        eventPublisher.publishInternalCollection(updateEvent)
    }
}
