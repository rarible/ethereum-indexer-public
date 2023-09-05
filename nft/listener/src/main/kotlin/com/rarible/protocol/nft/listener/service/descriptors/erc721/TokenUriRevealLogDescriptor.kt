package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.collection.TokenUriRevealEvent
import com.rarible.protocol.nft.core.model.TokenUriReveal
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

class TokenUriRevealLogDescriptor : LogEventDescriptor<TokenUriReveal> {
    override val collection: String = NftHistoryRepository.COLLECTION
    override val topic: Word = TokenUriRevealEvent.id()

    override fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Long,
        index: Int,
        totalLogs: Int
    ): Publisher<TokenUriReveal> {
        TODO("Not yet implemented")
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        TODO("Not yet implemented")
    }
}
