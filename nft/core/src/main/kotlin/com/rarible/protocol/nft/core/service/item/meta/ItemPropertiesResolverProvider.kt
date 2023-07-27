package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.service.item.meta.descriptors.AavegotchiPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.AlchemistCruciblePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ArtBlocksPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoKittiesPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.EmblemVaultV2Resolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.EnsDomainsPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.HashmasksPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.HegicPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.LazyItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.LootPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.MutantsBoredApeYachtClubPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OriginalUrlPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PegaxyPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RectguyCatsPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.StonerCatsPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.WaifusionPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.YInsurePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.polygon.SandboxPropertiesResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

// TODO separate ethereum and polygon resolvers
@Component

class ItemPropertiesResolverProvider(
    raribleResolver: RariblePropertiesResolver,
    cryptoKittiesResolver: CryptoKittiesPropertiesResolver,
    mutantsBoredApeYachtClubPropertiesResolver: MutantsBoredApeYachtClubPropertiesResolver,
    lootResolver: LootPropertiesResolver,
    yInsureResolver: YInsurePropertiesResolver,
    hegicResolver: HegicPropertiesResolver,
    waifusionResolver: WaifusionPropertiesResolver,
    cryptoPunksResolver: CryptoPunksPropertiesResolver,
    ensDomainsPropertiesResolver: EnsDomainsPropertiesResolver,
    hashmasksPropertiesResolver: HashmasksPropertiesResolver,
    lazyItemPropertiesResolver: LazyItemPropertiesResolver,
    stonerCatsPropertiesResolver: StonerCatsPropertiesResolver,
    emblemVaultV2Resolver: EmblemVaultV2Resolver,
    aavegotchiPropertiesResolver: AavegotchiPropertiesResolver,
    alchemistCruciblePropertiesResolver: AlchemistCruciblePropertiesResolver,
    pegaxyPropertiesResolver: PegaxyPropertiesResolver,
    artBlocksPropertiesResolver: ArtBlocksPropertiesResolver,
    rectguyCatsPropertiesResolver: RectguyCatsPropertiesResolver,
    sandboxPropertiesResolver: SandboxPropertiesResolver,
    originalUrlPropertiesResolver: OriginalUrlPropertiesResolver,
) {
    /**
     * Must not be returned from the [orderedResolvers]
     */
    @Autowired
    lateinit var openSeaResolver: OpenSeaPropertiesResolver

    val orderedResolvers: List<ItemPropertiesResolver> = listOf(
        // Custom resolvers, should be first in the list
        yInsureResolver,
        hegicResolver,
        waifusionResolver,
        hashmasksPropertiesResolver,
        cryptoPunksResolver,
        cryptoKittiesResolver,
        ensDomainsPropertiesResolver,
        mutantsBoredApeYachtClubPropertiesResolver,
        lootResolver,
        stonerCatsPropertiesResolver,
        emblemVaultV2Resolver,
        aavegotchiPropertiesResolver,
        alchemistCruciblePropertiesResolver,
        pegaxyPropertiesResolver,
        artBlocksPropertiesResolver,
        rectguyCatsPropertiesResolver,
        sandboxPropertiesResolver,
        originalUrlPropertiesResolver,
        // Default resolvers, should be last in the list
        lazyItemPropertiesResolver,
        raribleResolver
    )
}
