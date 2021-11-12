package com.rarible.protocol.nft.core.service.token

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.span.SpanType
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.SERVICE, subtype = "token-update")
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
