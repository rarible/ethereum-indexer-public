package com.rarible.protocol.nftorder.listener.test.mock

import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
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

}