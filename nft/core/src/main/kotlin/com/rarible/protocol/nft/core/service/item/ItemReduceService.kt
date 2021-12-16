package com.rarible.protocol.nft.core.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.ItemId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

interface ItemReduceService {
    fun onItemHistories(logs: List<LogEvent>): Mono<Void>

    fun update(token: Address? = null, tokenId: EthUInt256? = null, from: ItemId? = null): Flux<ItemId>
}

