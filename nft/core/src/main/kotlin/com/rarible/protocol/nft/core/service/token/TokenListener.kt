package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.converters.dto.ExtendedCollectionDtoConverter
import com.rarible.protocol.nft.core.model.ExtendedToken
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.service.token.meta.TokenMetaService
import org.springframework.stereotype.Component
import java.util.*

@Component
class TokenListener(
    private val eventPublisher: ProtocolNftEventPublisher,
    private val collectionDtoConverter: ExtendedCollectionDtoConverter,
    private val tokenMetaService: TokenMetaService
) {
    suspend fun onTokenChanged(token: Token) {
        val meta = tokenMetaService.get(token.id)
        val updateEvent = NftCollectionUpdateEventDto(
            eventId = token.lastEventId ?: UUID.randomUUID().toString(),
            id = token.id,
            collection = collectionDtoConverter.convert(ExtendedToken(token, meta))
        )
        eventPublisher.publish(updateEvent)
    }
}
