package com.rarible.protocol.nft.listener.service.token

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import scalether.domain.Address

@Service
class TokenService(
    private val tokenRepository: TokenRepository
) {
    private val map: MutableMap<Address, TokenStandard> = mutableMapOf()

    fun get(id: Address): Mono<Token> = tokenRepository.findById(id)

    fun getTokenStandard(token: Address): Mono<TokenStandard> {
        val result = map[token]
        return if (result != null) {
            Mono.just(result)
        } else {
            tokenRepository.findById(token)
                .map { it.standard }
                .switchIfEmpty { Mono.just(TokenStandard.NONE) }
                .doOnNext { map[token] = it }
        }
    }
}