package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.Token
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import scalether.domain.Address

@Primary
@Component
class FeaturedTokenReduceService(
    private val reducerV1: TokenReduceServiceV1,
    private val reducerV2: TokenReduceServiceV2,
    private var featureFlags: FeatureFlags
) : TokenReduceService {

    override suspend fun updateToken(address: Address): Token? {
        return getReducer().updateToken(address)
    }

    override fun update(address: Address): Flux<Token> {
        return getReducer().update(address)
    }

    fun getReducer(): TokenReduceService {
        return when (featureFlags.reduceVersion) {
            ReduceVersion.V1 -> reducerV1
            ReduceVersion.V2 -> reducerV2
        }
    }
}
