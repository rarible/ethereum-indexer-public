package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.OpenSeaPropertiesResolverTest.Companion.createExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.StonerCatsPropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class StonerCatsPropertiesResolverTest : BasePropertiesResolverTest() {

    private val httpClient = createExternalHttpClient()

    private val rariblePropertiesResolver: RariblePropertiesResolver = RariblePropertiesResolver(
        sender = createSender(),
        tokenRepository = tokenRepository,
        ipfsService = ipfsService,
        requestTimeout = 20000,
        externalHttpClient = httpClient
    )

    private val stonerCatsPropertiesResolver = StonerCatsPropertiesResolver(
        ipfsService,
        rariblePropertiesResolver,
        httpClient
    )

    @Test
    fun `stonercat image url from etag`() = runBlocking<Unit> {
        val address = StonerCatsPropertiesResolver.STONER_CAT_NFT_ADDRESS
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = stonerCatsPropertiesResolver.resolve(ItemId(address, EthUInt256.of(3709))).itemProperties!!

        // There is no need to check all fields, it is already tested in RariblePropertiesResolver
        assertThat(properties.name).isEqualTo("Stoner Cats #3709")
        assertThat(properties.image).isEqualTo(
            "${ipfsService.publicGateway}/ipfs/bafybeigvfr47mucanjlsqoz2dti5ariurqgvpergl5vkhgpvihskyj4t5m"
        )
    }

}
