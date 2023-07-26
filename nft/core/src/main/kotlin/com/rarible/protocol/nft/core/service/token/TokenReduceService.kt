package com.rarible.protocol.nft.core.service.token

import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.converters.model.TokenEventConverter
import com.rarible.protocol.nft.core.model.CollectionEvent
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.service.token.reduce.TokenReducer
import com.rarible.protocol.nft.core.service.token.reduce.TokenTemplateProvider
import com.rarible.protocol.nft.core.service.token.reduce.TokenUpdateService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import scalether.domain.Address

@Component
class TokenReduceService(
    private val tokenReducer: TokenReducer,
    private val tokenUpdateService: TokenUpdateService,
    private val tokenTemplateProvider: TokenTemplateProvider,
    private val tokenHistoryRepository: NftHistoryRepository,
) {

    suspend fun reduce(address: Address): Token? {
        return tokenHistoryRepository.findAllByCollection(address)
            .windowUntilChanged { (it.data as CollectionEvent).id }
            .concatMap { reduce(it) }
            .awaitFirstOrNull()
    }

    private fun reduce(logs: Flux<LogEvent>) = mono {
        // Expect not too many events for single Collection
        val events = logs.collectList().awaitFirst().mapNotNull { TokenEventConverter.convert(it) }
        val tokenId = events.firstOrNull()?.entityId ?: return@mono null
        reduce(Address.apply(tokenId), events)
    }

    private suspend fun reduce(tokenId: Address, events: List<TokenEvent>): Token = optimisticLock {
        val entity = tokenUpdateService.get(tokenId) ?: tokenTemplateProvider.getEntityTemplate(tokenId)
        val result = events.fold(entity) { e, event -> tokenReducer.reduce(e, event) }
        tokenUpdateService.update(result)
    }
}
