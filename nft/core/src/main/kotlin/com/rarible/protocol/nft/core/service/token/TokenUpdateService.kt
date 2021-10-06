package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.nft.core.repository.TokenRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenUpdateService(
    private val tokenRepository: TokenRepository,
    private val tokenReduceService: TokenReduceService,
    private val tokenListener: TokenListener
) {
    suspend fun update(tokenId: Address) {
        val token = tokenRepository.findById(tokenId).awaitFirstOrNull()
        val updatedToken = tokenReduceService.updateToken(tokenId)
        if (updatedToken != null && token?.lastEventId != updatedToken.lastEventId) {
            tokenListener.onTokenChanged(updatedToken)
        }
    }
}
