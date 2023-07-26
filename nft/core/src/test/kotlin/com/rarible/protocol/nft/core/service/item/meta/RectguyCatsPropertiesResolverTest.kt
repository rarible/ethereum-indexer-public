package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RectguyCatsPropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class RectguyCatsPropertiesResolverTest : BasePropertiesResolverTest() {

    private val rectguyCatsPropertiesResolver = RectguyCatsPropertiesResolver(
        urlParser = urlParser,
        raribleResolver = rariblePropertiesResolver
    )

    @Test
    fun `rectguy ipfs gateway replaced`() = runBlocking<Unit> {
        val address = RectguyCatsPropertiesResolver.RECTGUY_NFT_ADDRESS
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = rectguyCatsPropertiesResolver.resolve(ItemId(address, EthUInt256.of(7311)))!!

        // Should be replaced by schema-IPFS URL (their gateway is not available anymore)
        // "https://rektguy.mypinata.cloud/ipfs/QmRuj3fqWkZkuruTkPgGSvSdTdjyAMiXyBDPQ5oFer43Rq/7311.gif"
        assertThat(properties.content.imageOriginal!!.url)
            .isEqualTo("ipfs://QmRuj3fqWkZkuruTkPgGSvSdTdjyAMiXyBDPQ5oFer43Rq/7311.gif")
    }
}
