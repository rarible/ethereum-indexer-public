package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.collection.TokenUriRevealEvent
import com.rarible.protocol.nft.core.model.TokenUriReveal
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
class TokenUriRevealLogDescriptor(
    ignoredTokenResolver: IgnoredTokenResolver,
) : LogEventDescriptor<TokenUriReveal> {
    private val skipContracts = ignoredTokenResolver.resolve()
    override val collection: String = NftHistoryRepository.COLLECTION
    override val topic: Word = TokenUriRevealEvent.id()

    override fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Long,
        index: Int,
        totalLogs: Int
    ): Publisher<TokenUriReveal> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }
        val e = TokenUriRevealEvent.apply(log)
        return Mono.just(TokenUriReveal(log.address(), e.revealedUri()))
    }

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()
}
