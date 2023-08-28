package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.token.TokenService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenUpdateService(
    private val tokenService: TokenService
) : EntityService<Address, Token, TokenEvent> {

    override suspend fun get(id: Address): Token? {
        return tokenService.getToken(id)
    }

    override suspend fun update(entity: Token, event: TokenEvent?): Token {
        val result = tokenService.saveToken(entity, event)
        logUpdatedToken(entity)
        return result
    }

    private fun logUpdatedToken(token: Token) {
        logger.info(buildString {
            append("Updated token: ")
            append("id=${token.id}, ")
            append("version=${token.version}, ")
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
