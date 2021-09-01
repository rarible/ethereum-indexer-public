package com.rarible.protocol.nftorder.api.controller

import com.rarible.protocol.nftorder.api.client.NftOrderLazyMintControllerApi
import com.rarible.protocol.nftorder.api.test.AbstractFunctionalTest
import com.rarible.protocol.nftorder.api.test.FunctionalTest
import com.rarible.protocol.nftorder.core.test.data.assertItemDtoAndNftDtoEquals
import com.rarible.protocol.nftorder.listener.test.mock.data.randomItemId
import com.rarible.protocol.nftorder.listener.test.mock.data.randomLazyErc721Dto
import com.rarible.protocol.nftorder.listener.test.mock.data.randomNftItemDto
import com.rarible.protocol.nftorder.listener.test.mock.data.randomOrderDto
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FunctionalTest
internal class LazyMintControllerFt : AbstractFunctionalTest() {

    @Autowired
    private lateinit var nftOrderLazyMintControllerApi: NftOrderLazyMintControllerApi

    @Test
    @Disabled
    fun `mint asset`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val lazyNft = randomLazyErc721Dto(itemId)
        val nftItemDto = randomNftItemDto(itemId)
        val bestSell = randomOrderDto(itemId)
        val bestBid = randomOrderDto(itemId)

        nftLazyMintControllerApiMock.mockMintNftAsset(lazyNft, nftItemDto)
        lockControllerApiMock.mockIsUnlockable(itemId, true)
        orderControllerApiMock.mockGetSellOrdersByItem(itemId, bestSell)
        orderControllerApiMock.mockGetBidOrdersByItem(itemId, bestBid)

        val result = nftOrderLazyMintControllerApi.mintNftOrderAsset(lazyNft)
            .awaitFirst()

        assertThat(result.bestBidOrder).isEqualTo(bestBid)
        assertThat(result.bestSellOrder).isEqualTo(bestSell)
        assertThat(result.unlockable).isTrue()
        assertItemDtoAndNftDtoEquals(result, nftItemDto)
    }

}
