package com.rarible.protocol.nft.core.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceVersion
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Primary
@Component
class FeaturedItemReduceService(
    private val reducerV1: ItemReduceServiceV1,
    private val reducerV2: ItemReduceServiceV2,
    private var featureFlags: FeatureFlags
) : ItemReduceService {

    override fun onItemHistories(logs: List<LogEvent>): Mono<Void> {
        return getReducer().onItemHistories(logs)
    }

    override fun update(token: Address?, tokenId: EthUInt256?, from: ItemId?): Flux<ItemId> {
        return getReducer().update(token, tokenId, from)
    }

    fun getReducer(): ItemReduceService {
        return when (featureFlags.reduceVersion) {
            ReduceVersion.V1 -> reducerV1
            ReduceVersion.V2 -> reducerV2
        }
    }
}
