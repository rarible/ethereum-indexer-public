package com.rarible.protocol.nft.listener.service.descriptors.creators

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.creators.CreatorsEvent
import com.rarible.protocol.contracts.creators.CreatorsIndexedEvent
import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Component
class CreatorsLogDescriptor(properties: NftListenerProperties) : ItemHistoryLogEventDescriptor<ItemCreators> {
    private val skipContracts = properties.skipContracts.map { Address.apply(it) }

    override val topic: Word = CreatorsEvent.id()

    override fun convert(log: Log, date: Instant): Mono<ItemCreators> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }

        val event = parseCreatorsEvent(log)
        return ItemCreators(
            token = log.address(),
            tokenId = EthUInt256.of(event.tokenId()),
            date = date,
            creators = event.creators().map { Part(it._1(), it._2().toInt()) }
        ).toMono()
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(emptyList())
    }
}

fun parseCreatorsEvent(log: Log): CreatorsEvent = if (log.topics().size() == 1) {
    CreatorsEvent.apply(log)
} else {
    CreatorsIndexedEvent.apply(log)
}
