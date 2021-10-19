package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.erc721.OwnershipTransferredEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log

@Service
class CollectionOwnershipTransferLogDescriptor : LogEventDescriptor<CollectionOwnershipTransferred> {

    override val topic: Word = OwnershipTransferredEvent.id()

    override fun convert(log: Log, timestamp: Long): Publisher<CollectionOwnershipTransferred> {
        if (log.topics().length() != 3) {
            // Ignore similar events without indexed fields.
            return Mono.empty()
        }
        val event = OwnershipTransferredEvent.apply(log)
        val previousOwner = event.previousOwner()
        val newOwner = event.newOwner()
        return CollectionOwnershipTransferred(
            id = log.address(),
            previousOwner = previousOwner,
            newOwner = newOwner
        ).toMono()
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(emptyList())

    override val collection: String = NftHistoryRepository.COLLECTION
}
