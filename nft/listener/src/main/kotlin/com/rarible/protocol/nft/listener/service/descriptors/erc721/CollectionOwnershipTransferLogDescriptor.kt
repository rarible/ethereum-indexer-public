package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.erc721.OwnershipTransferredEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
class CollectionOwnershipTransferLogDescriptor(
    private val tokenRegistrationService: TokenRegistrationService
) : LogEventDescriptor<CollectionOwnershipTransferred> {

    override val topic: Word = OwnershipTransferredEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long): Publisher<CollectionOwnershipTransferred> {
        if (log.topics().length() != 3) {
            // Ignore similar events without indexed fields.
            return Mono.empty()
        }
        return tokenRegistrationService.getTokenStandard(log.address()).flatMap { standard ->
            if (standard != TokenStandard.ERC721 && standard != TokenStandard.ERC1155) {
                Mono.empty<CollectionOwnershipTransferred>()
            } else {
                val event = OwnershipTransferredEvent.apply(log)
                val previousOwner = event.previousOwner()
                val newOwner = event.newOwner()
                CollectionOwnershipTransferred(
                    id = log.address(),
                    previousOwner = previousOwner,
                    newOwner = newOwner
                ).toMono()
            }
        }
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(emptyList())

    override val collection: String = NftHistoryRepository.COLLECTION
}
