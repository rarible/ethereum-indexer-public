package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.protocol.nft.core.model.TokenEvent
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenIdService : EntityIdService<TokenEvent, Address> {
    override fun getEntityId(event: TokenEvent): Address {
        return Address.apply(event.entityId)
    }
}
