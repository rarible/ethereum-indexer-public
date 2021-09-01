package com.rarible.protocol.nftorder.api.controller

import com.rarible.protocol.nftorder.api.client.NftOrderLazyMintControllerApi
import com.rarible.protocol.nftorder.api.test.AbstractFunctionalTest
import com.rarible.protocol.nftorder.api.test.FunctionalTest
import com.rarible.protocol.nftorder.core.test.data.assertItemDtoAndNftDtoEquals
import com.rarible.protocol.nftorder.listener.test.mock.data.randomItemId
import com.rarible.protocol.nftorder.listener.test.mock.data.randomLazyErc721Dto
import com.rarible.protocol.nftorder.listener.test.mock.data.randomNftItemDto
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FunctionalTest
internal class LazyMintControllerFt : AbstractFunctionalTest() {

    @Autowired
    private lateinit var nftOrderLazyMintControllerApi: NftOrderLazyMintControllerApi

    @Test
    fun `mint asset`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val lazyNft = randomLazyErc721Dto(itemId)
        val nftItemDto = randomNftItemDto(itemId)

        nftLazyMintControllerApiMock.mockMintNftAsset(lazyNft, nftItemDto)

        val result = nftOrderLazyMintControllerApi.mintNftOrderAsset(lazyNft)
            .awaitFirst()

        assertThat(result.bestBidOrder).isNull()
        assertThat(result.bestSellOrder).isNull()
        assertThat(result.unlockable).isFalse()
        assertItemDtoAndNftDtoEquals(result, nftItemDto)
    }

}