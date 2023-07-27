package com.rarible.protocol.nft.core.service.item.meta

import com.mongodb.assertions.Assertions.assertTrue
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.descriptors.EmblemVaultV2Resolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.EmblemVaultV2Resolver.Companion.EMBLEM_VAULT_V2_ADDRESS
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@ItemMetaTest
class EmblemVaultV2ResolverTest : BasePropertiesResolverTest() {

    private val emblemVaultV2Resolver = EmblemVaultV2Resolver(
        urlService = urlService,
        raribleResolver = rariblePropertiesResolver,
        externalHttpClient = externalHttpClient
    )

    @BeforeEach
    fun mock() = mockTokenStandard(EMBLEM_VAULT_V2_ADDRESS, TokenStandard.ERC721)

    @Test
    fun `test resolve base64 jpeg`(): Unit = runBlocking {
        val properties = emblemVaultV2Resolver.resolve(
            ItemId(
                EMBLEM_VAULT_V2_ADDRESS,
                EthUInt256.of(56394538315072181)
            )
        )!!
        assertThat(properties.name).isEqualTo("SISTNCHAPEPE 2018")
        assertTrue(properties.content.imageOriginal!!.url.startsWith("data:image/jpeg;base64,"))
    }

    @Test
    fun `test resolve base64 gif`(): Unit = runBlocking {
        val properties = emblemVaultV2Resolver.resolve(
            ItemId(
                EMBLEM_VAULT_V2_ADDRESS,
                EthUInt256.of(55344981240773651)
            )
        )!!
        assertThat(properties.name).isEqualTo("GIVEKUDOS Rare Pepe Collection Series 8 Card 43")
        assertTrue(properties.content.imageOriginal!!.url.startsWith("data:image/gif;base64,"))
    }

    @Test
    fun `test resolve simple image URL`(): Unit = runBlocking {
        val properties = emblemVaultV2Resolver.resolve(
            ItemId(
                EMBLEM_VAULT_V2_ADDRESS,
                EthUInt256.of(86161224632699541)
            )
        )!!
        assertThat(properties.name).isEqualTo("TESTNETPEPE | Series 9, Card 36 | Rare Pepe Collection | 2016 Counterparty XCP NFT Asset")
        assertThat(properties.content.imageOriginal!!.url).isEqualTo("https://xchain.io/img/cards/TESTNETPEPE.jpg")
    }
}
