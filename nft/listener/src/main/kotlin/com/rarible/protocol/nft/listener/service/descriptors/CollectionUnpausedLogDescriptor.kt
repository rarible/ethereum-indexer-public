package com.rarible.protocol.nft.listener.service.descriptors

import com.rarible.contracts.pausable.UnpausedEvent
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.nft.core.model.CollectionPaused
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Component
class CollectionUnpausedLogDescriptor(
    ignoredTokenResolver: IgnoredTokenResolver
) : LogEventDescriptor<CollectionPaused> {
    private val skipContracts = ignoredTokenResolver.resolve()
    override val collection: String = NftHistoryRepository.COLLECTION
    override val topic: Word = UnpausedEvent.id()

    override fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Long,
        index: Int,
        totalLogs: Int
    ): Publisher<CollectionPaused> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }
        return Mono.just(CollectionPaused(log.address(), false))
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.empty()
}
