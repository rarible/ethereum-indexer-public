package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nftorder.core.model.ItemId
import io.mockk.every
import reactor.core.publisher.Mono

class NftItemControllerApiMock(
    private val nftItemControllerApi: NftItemControllerApi
) {

    fun mockGetNftItemById(itemId: ItemId, returnItem: NftItemDto?) {
        every {
            nftItemControllerApi.getNftItemById(itemId.decimalStringValue)
        } returns (if (returnItem == null) Mono.empty() else Mono.just(returnItem))
    }

    fun mockGetNftItemMetaById(itemId: ItemId, returnItemMeta: NftItemMetaDto) {
        every {
            nftItemControllerApi.getNftItemMetaById(itemId.decimalStringValue)
        } returns Mono.just(returnItemMeta)
    }

    fun mockGetNftItemById(itemId: ItemId, status: Int, error: Any) {
        every {
            nftItemControllerApi.getNftItemById(itemId.decimalStringValue)
        } throws WebClientExceptionMock.mock(status, error)
    }

    fun mockGetNftAllItems(
        continuation: String,
        size: Int,
        showDeleted: Boolean,
        lastUpdatedFrom: Long,
        lastUpdatedTo: Long,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftAllItems(
                continuation,
                size,
                showDeleted,
                lastUpdatedFrom,
                lastUpdatedTo
            )
        } returns Mono.just(NftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByOwner(owner: String, vararg returnItems: NftItemDto) {
        every {
            nftItemControllerApi.getNftItemsByOwner(owner, null, null)
        } returns Mono.just(NftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByCollection(collection: String, vararg returnItems: NftItemDto) {
        every {
            nftItemControllerApi.getNftItemsByCollection(collection, null, null, null)
        } returns Mono.just(NftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByCreator(creator: String, vararg returnItems: NftItemDto) {
        every {
            nftItemControllerApi.getNftItemsByCreator(creator, null, null)
        } returns Mono.just(NftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetLazyItemById(itemId: String, returnValue: LazyNftDto) {
        every {
            nftItemControllerApi.getNftLazyItemById(itemId)
        } returns Mono.just(returnValue)
    }

    fun mockGetItemMetaById(itemId: String, returnValue: NftItemMetaDto) {
        every {
            nftItemControllerApi.getNftItemMetaById(itemId)
        } returns Mono.just(returnValue)
    }

}
