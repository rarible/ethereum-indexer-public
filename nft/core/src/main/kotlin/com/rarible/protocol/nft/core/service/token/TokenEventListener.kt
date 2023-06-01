package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.converters.dto.CollectionDtoConverter
import com.rarible.protocol.nft.core.misc.addOut
import com.rarible.protocol.nft.core.misc.nftOffchainEventMarks
import com.rarible.protocol.nft.core.misc.toDto
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TokenEventListener(
    private val eventPublisher: ProtocolNftEventPublisher
) {

    suspend fun onTokenChanged(token: Token, event: TokenEvent? = null) {
        if (!token.standard.isNotIgnorable()) {
            return
        }

        val updateEvent = NftCollectionUpdateEventDto(
            eventId = token.lastEventId ?: UUID.randomUUID().toString(),
            id = token.id,
            collection = CollectionDtoConverter.convert(token),
            eventTimeMarks = (event?.eventTimeMarks ?: nftOffchainEventMarks()).addOut().toDto()
        )
        eventPublisher.publish(updateEvent)
    }
}
