package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import io.mockk.every
import reactor.core.publisher.Mono

class NftOwnershipControllerApiMock(
    private val nftOwnershipControllerApi: NftOwnershipControllerApi
) {

    fun mockGetNftOwnershipById(ownershipId: OwnershipId, returnOwnership: NftOwnershipDto?) {
        every {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId.stringValue)
        } returns (if (returnOwnership == null) Mono.empty() else Mono.just(returnOwnership))
    }

    fun mockGetNftOwnershipById(ownershipId: OwnershipId, status: Int, error: Any) {
        every {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId.stringValue)
        } throws WebClientExceptionMock.mock(status, error)
    }

    fun mockGetNftOwnershipsByItem(itemId: ItemId, vararg returnOwnerships: NftOwnershipDto) {
        every {
            nftOwnershipControllerApi.getNftOwnershipsByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                null,
                null
            )
        } returns Mono.just(NftOwnershipsDto(returnOwnerships.size.toLong(), null, returnOwnerships.asList()))
    }

    fun mockGetNftAllOwnerships(continuation: String, size: Int, vararg returnOwnerships: NftOwnershipDto) {
        every {
            nftOwnershipControllerApi.getNftAllOwnerships(
                continuation,
                size
            )
        } returns Mono.just(NftOwnershipsDto(returnOwnerships.size.toLong(), null, returnOwnerships.asList()))
    }

}