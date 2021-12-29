package com.rarible.protocol.nft.api.service.mint

import com.rarible.protocol.nft.core.model.*
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class FeaturedMintService(
    private val legacyMintService: LegacyMintService,
    private val mintService: MintServiceImp,
    private var featureFlags: FeatureFlags
) : MintService {

    override suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): Item {
        return getService().createLazyNft(lazyItemHistory)
    }

    override suspend fun burnLazyMint(itemId: ItemId) {
        return getService().burnLazyMint(itemId)

    }

    private fun getService(): MintService {
        return when (featureFlags.reduceVersion) {
            ReduceVersion.V1 -> legacyMintService
            ReduceVersion.V2 -> mintService
        }
    }
}