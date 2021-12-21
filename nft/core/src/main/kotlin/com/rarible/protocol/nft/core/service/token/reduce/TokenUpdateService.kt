package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenEventListener
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenUpdateService(
    private val tokenRepository: TokenRepository,
    private val tokenListener: TokenEventListener
) : EntityService<Address, Token> {

    override suspend fun get(id: Address): Token? {
        return tokenRepository.findById(id).awaitFirstOrNull()
    }

    override suspend fun update(entity: Token): Token {
        val savedToken = tokenRepository.save(entity).awaitFirst()
        tokenListener.onTokenChanged(savedToken)
        logUpdatedToken(savedToken)
        return savedToken
    }

    private fun logUpdatedToken(token: Token) {
        logger.info(buildString {
            append("Updated token: ")
            append("id=${token.id}, ")
            append("owner=${token.owner}, ")
            append("symbol=${token.symbol}, ")
            append("status=${token.status}, ")
            append("features=${token.features}, ")
            append("standard=${token.standard}")
            append("deleted=${token.deleted}")
            append("revertableEvents=${token.revertableEvents}, ")
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenUpdateService::class.java)
    }
}
