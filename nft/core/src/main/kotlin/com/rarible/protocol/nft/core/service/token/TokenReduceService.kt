package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.nft.core.model.Token
import reactor.core.publisher.Flux
import scalether.domain.Address

interface TokenReduceService {
    suspend fun updateToken(address: Address): Token?

    fun update(address: Address): Flux<Token>
}
