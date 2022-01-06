package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoKittiesPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.HashmasksPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.HegicPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.LazyItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.LootPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaLegacyCachePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PendingLogItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RaribleLegacyCachePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.WaifusionPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.YInsurePropertiesResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ItemPropertiesResolverProvider(
    openSeaLegacyCachePropertiesResolver: OpenSeaLegacyCachePropertiesResolver,
    raribleLegacyCachePropertiesResolver: RaribleLegacyCachePropertiesResolver,

    raribleResolver: RariblePropertiesResolver,
    cryptoKittiesResolver: CryptoKittiesPropertiesResolver,
    lootResolver: LootPropertiesResolver,
    yInsureResolver: YInsurePropertiesResolver,
    hegicResolver: HegicPropertiesResolver,
    waifusionResolver: WaifusionPropertiesResolver,
    cryptoPunksResolver: CryptoPunksPropertiesResolver,
    pendingLogItemPropertiesResolver: PendingLogItemPropertiesResolver,
    hashmasksPropertiesResolver: HashmasksPropertiesResolver,
    lazyItemPropertiesResolver: LazyItemPropertiesResolver
) {
    /**
     * Must not be returned from the [orderedResolvers]
     */
    @Autowired
    lateinit var openSeaResolver: OpenSeaPropertiesResolver

    val orderedResolvers: List<ItemPropertiesResolver> = listOf(
        openSeaLegacyCachePropertiesResolver,
        raribleLegacyCachePropertiesResolver,

        yInsureResolver,
        hegicResolver,
        waifusionResolver,
        hashmasksPropertiesResolver,
        cryptoPunksResolver,
        cryptoKittiesResolver,
        lootResolver,
        lazyItemPropertiesResolver,
        pendingLogItemPropertiesResolver,
        raribleResolver
    )
}
