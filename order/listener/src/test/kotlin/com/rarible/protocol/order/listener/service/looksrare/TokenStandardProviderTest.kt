package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.service.nft.NftCollectionApiService
import com.rarible.protocol.order.listener.data.createNftCollectionDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TokenStandardProviderTest {
    private val nftCollectionApiService = mockk<NftCollectionApiService>()
    private val provider = TokenStandardProvider(nftCollectionApiService)

    @Test
    fun `should get standard for erc721`() = runBlocking<Unit> {
        val token = randomAddress()
        val collection = createNftCollectionDto().copy(type = NftCollectionDto.Type.ERC721)
        coEvery { nftCollectionApiService.getNftCollectionById(token) } returns collection
        val standard = provider.getTokenStandard(token)
        assertThat(standard).isEqualTo(TokenStandard.ERC721)
    }

    @Test
    fun `should get standard for erc1155`() = runBlocking<Unit> {
        val token = randomAddress()
        val collection = createNftCollectionDto().copy(type = NftCollectionDto.Type.ERC1155)
        coEvery { nftCollectionApiService.getNftCollectionById(token) } returns collection
        val standard = provider.getTokenStandard(token)
        assertThat(standard).isEqualTo(TokenStandard.ERC1155)
    }
}
