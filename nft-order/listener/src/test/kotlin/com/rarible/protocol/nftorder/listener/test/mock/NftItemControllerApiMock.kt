package com.rarible.protocol.nftorder.listener.test.mock

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nftorder.core.model.ItemId
import io.mockk.every
import reactor.core.publisher.Mono

class NftItemControllerApiMock(
    private val nftItemControllerApi: NftItemControllerApi
) {

    fun mockGetNftItemById(itemId: ItemId, returnItem: NftItemDto) {
        every {
            nftItemControllerApi.getNftItemById(itemId.decimalStringValue)
        } returns (if (returnItem == null) Mono.empty() else Mono.just(returnItem))

    }

    fun mockGetItemMetaById(itemId: String?, metaDto: NftItemMetaDto) {
        every {
            nftItemControllerApi.getNftItemMetaById(itemId ?: any())
        } returns Mono.just(metaDto)
    }
}
