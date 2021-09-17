package com.rarible.protocol.nftorder.api.controller

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.EthereumApiErrorEntityNotFoundDto
import com.rarible.protocol.dto.EthereumApiErrorServerErrorDto
import com.rarible.protocol.nftorder.api.client.NftOrderOwnershipControllerApi
import com.rarible.protocol.nftorder.api.test.AbstractFunctionalTest
import com.rarible.protocol.nftorder.api.test.FunctionalTest
import com.rarible.protocol.nftorder.core.repository.OwnershipRepository
import com.rarible.protocol.nftorder.core.test.data.assertOwnershipAndDtoEquals
import com.rarible.protocol.nftorder.core.test.data.assertOwnershipDtoAndNftDtoEquals
import com.rarible.protocol.nftorder.listener.test.mock.data.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

@FunctionalTest
internal class OwnershipControllerFt : AbstractFunctionalTest() {

    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    lateinit var nftOrderOwnershipControllerApi: NftOrderOwnershipControllerApi

    @Test
    fun `get ownership by id - not synced`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val ownershipId = randomOwnershipId(itemId)
        val nftOwnership = randomNftOwnershipDto(ownershipId)

        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, nftOwnership)

        val result = nftOrderOwnershipControllerApi
            .getNftOrderOwnershipById(ownershipId.decimalStringValue)
            .awaitFirst()!!

        // If we don't have Ownership in DB it means there is no enrich data, so we just return Ownership from indexer
        assertThat(result.bestSellOrder).isEqualTo(null)
        assertOwnershipDtoAndNftDtoEquals(result, nftOwnership)
    }

    @Test
    fun `get ownership by id - synced`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val ownershipId = randomOwnershipId(itemId)
        val orderDto = randomOrderDto(itemId, ownershipId.owner)
        val ownership = randomOwnership(itemId).copy(owner = ownershipId.owner, bestSellOrder = orderDto)
        ownershipRepository.save(ownership)

        val result = nftOrderOwnershipControllerApi
            .getNftOrderOwnershipById(ownership.id.decimalStringValue)
            .awaitFirst()!!

        assertThat(result.bestSellOrder).isEqualTo(orderDto)
        assertOwnershipAndDtoEquals(ownership, result)
    }


    @Test
    fun `get ownership by id - not found`() = runBlocking<Unit> {
        val ownershipId = randomOwnershipId()
        val nftApiError = EthereumApiErrorEntityNotFoundDto(EthereumApiErrorEntityNotFoundDto.Code.NOT_FOUND, "123");

        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, 404, nftApiError)

        val ex = assertThrows<NftOrderOwnershipControllerApi.ErrorGetNftOrderOwnershipById> {
            nftOrderOwnershipControllerApi
                .getNftOrderOwnershipById(ownershipId.decimalStringValue)
                .block()
        }

        assertThat(ex.rawStatusCode).isEqualTo(404)
        assertThat(ex.on404).isEqualTo(nftApiError)
    }

    @Test
    fun `get ownership by id - unexpected api error`() = runBlocking<Unit> {
        val ownershipId = randomOwnershipId()
        val nftApiError = EthereumApiErrorServerErrorDto(EthereumApiErrorServerErrorDto.Code.UNKNOWN, "321")

        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, 500, nftApiError)

        val ex = assertThrows<NftOrderOwnershipControllerApi.ErrorGetNftOrderOwnershipById> {
            nftOrderOwnershipControllerApi
                .getNftOrderOwnershipById(ownershipId.decimalStringValue)
                .block()
        }

        assertThat(ex.rawStatusCode).isEqualTo(500)
        assertThat(ex.on500).isEqualTo(nftApiError)
    }

    @Test
    fun `get ownership by id - unparseable id`() = runBlocking<Unit> {
        val ex = assertThrows<NftOrderOwnershipControllerApi.ErrorGetNftOrderOwnershipById> {
            nftOrderOwnershipControllerApi
                .getNftOrderOwnershipById("unparseable value")
                .block()
        }

        assertThat(ex.on500).isNotNull
    }

    @Test
    fun `get ownerships by item`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftOwnership1 = randomNftOwnershipDto(itemId)
        val nftOwnership2 = randomNftOwnershipDto(itemId)

        nftOwnershipControllerApiMock.mockGetNftOwnershipsByItem(itemId, nftOwnership1, nftOwnership2)

        val result = nftOrderOwnershipControllerApi.getNftOrderOwnershipsByItem(
            itemId.token.hex(),
            itemId.tokenId.value.toString(),
            null,
            null
        ).awaitFirstOrNull()!!

        assertThat(result.data.size).isEqualTo(2)
        assertOwnershipDtoAndNftDtoEquals(result.data[0], nftOwnership1)
        assertOwnershipDtoAndNftDtoEquals(result.data[1], nftOwnership2)
    }

    @Test
    fun `get all ownerships`() = runBlocking<Unit> {
        val nftOwnership1 = randomNftOwnershipDto()
        val nftOwnership2 = randomNftOwnershipDto()
        val nftOwnership3 = randomNftOwnershipDto()

        val continuation = randomString()
        val size = 25

        nftOwnershipControllerApiMock.mockGetNftAllOwnerships(
            continuation,
            size,
            nftOwnership1,
            nftOwnership2,
            nftOwnership3
        )

        val result = nftOrderOwnershipControllerApi.getNftOrderAllOwnerships(
            continuation,
            size
        ).awaitFirstOrNull()!!

        assertThat(result.data.size).isEqualTo(3)
        assertOwnershipDtoAndNftDtoEquals(result.data[0], nftOwnership1)
        assertOwnershipDtoAndNftDtoEquals(result.data[1], nftOwnership2)
        assertOwnershipDtoAndNftDtoEquals(result.data[2], nftOwnership3)
    }
}