package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.collection.CreateERC1155RaribleEvent
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
@CaptureSpan(type = SpanType.EVENT)
class CreateERC1155RaribleLogDescriptor : LogEventDescriptor<CreateCollection> {
    override val collection: String = NftHistoryRepository.COLLECTION

    override val topic: Word = CreateERC1155RaribleEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long): Mono<CreateCollection> {
        val e = CreateERC1155RaribleEvent.apply(log)

        return CreateCollection(
            id = e.log().address(),
            owner = e.owner(),
            name = e.name(),
            symbol = e.symbol()
        ).toMono()
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return emptyList<Address>().toMono()
    }
}

