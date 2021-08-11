package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.dto.NftTokenIdDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import io.mockk.every
import reactor.core.publisher.Mono
import scalether.domain.Address

class NftCollectionControllerApiMock(
    private val nftCollectionControllerApi: NftCollectionControllerApi
) {

    fun mockGenerateTokenId(collection: String, minter: String, returnValue: NftTokenIdDto) {
        every {
            nftCollectionControllerApi.generateNftTokenId(collection, minter)
        } returns Mono.just(returnValue)
    }

    fun mockGetCollectionById(collection: String, returnValue: NftCollectionDto) {
        every {
            nftCollectionControllerApi.getNftCollectionById(collection)
        } returns Mono.just(returnValue)
    }

    fun mockSearchAllCollections(continuation: String?, size: Int?, vararg returnValues: NftCollectionDto) {
        every {
            nftCollectionControllerApi.searchNftAllCollections(continuation, size)
        } returns Mono.just(NftCollectionsDto(returnValues.size.toLong(), null, returnValues.toList()))
    }

    fun mockSearchCollectionsByOwner(
        owner: Address,
        continuation: String?,
        size: Int?,
        vararg returnValues: NftCollectionDto
    ) {
        every {
            nftCollectionControllerApi.searchNftCollectionsByOwner(owner.hex(), continuation, size)
        } returns Mono.just(NftCollectionsDto(returnValues.size.toLong(), null, returnValues.toList()))
    }

}