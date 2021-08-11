package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
import io.mockk.every
import reactor.core.publisher.Mono

class NftLazyMintControllerApiMock(
    private val nftLazyMintControllerApi: NftLazyMintControllerApi
) {

    fun mockMintNftAsset(lazyNftDto: LazyNftDto, returnValue: NftItemDto) {
        every {
            nftLazyMintControllerApi.mintNftAsset(lazyNftDto)
        } returns Mono.just(returnValue)
    }

}