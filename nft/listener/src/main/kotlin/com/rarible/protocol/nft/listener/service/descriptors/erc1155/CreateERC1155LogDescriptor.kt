package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.collection.CreateERC1155_v1Event
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
class CreateERC1155LogDescriptor : LogEventDescriptor<CreateCollection> {
    override val collection: String = NftHistoryRepository.COLLECTION

    override val topic: Word = CreateERC1155_v1Event.id()

    override fun convert(log: Log, timestamp: Long): Mono<CreateCollection> {
        val event = CreateERC1155_v1Event.apply(log)
        return CreateCollection(
            id = event.log().address(),
            date = Instant.ofEpochSecond(timestamp),
            owner = event.creator(),
            name = event.name(),
            symbol = event.symbol()
        ).toMono()
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return emptyList<Address>().toMono()
    }
}
