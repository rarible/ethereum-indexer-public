package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenUpdateService(
    private val tokenRepository: TokenRepository,
    private val tokenReduceService: TokenReduceService,
    private val tokenRegistrationService: TokenRegistrationService,
    private val tokenListener: TokenEventListener
) {

    suspend fun getToken(tokenId: Address): Token? =
        tokenRepository.findById(tokenId).awaitFirstOrNull()

    suspend fun removeToken(tokenId: Address) {
        // TODO: we need to send "token deleted" Kafka event.
        tokenRepository.remove(tokenId).awaitFirstOrNull()
    }

    suspend fun setTokenStandard(tokenId: Address, standard: TokenStandard) {
        val savedToken = tokenRegistrationService.setTokenStandard(tokenId, standard)
        tokenListener.onTokenChanged(savedToken)
    }

    suspend fun update(tokenId: Address) {
        val token = tokenRepository.findById(tokenId).awaitFirstOrNull()
        val updatedToken = tokenReduceService.updateToken(tokenId)
        if (updatedToken != null && token?.lastEventId != updatedToken.lastEventId) {
            tokenListener.onTokenChanged(updatedToken)
        }
    }
}
