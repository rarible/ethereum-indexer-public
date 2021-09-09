package com.rarible.protocol.nftorder.api.controller

import com.rarible.core.test.data.randomString
import com.rarible.protocol.client.exception.ProtocolApiResponseException
import com.rarible.protocol.dto.NftIndexerApiErrorDto
import com.rarible.protocol.dto.NftOrderApiErrorDto
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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.charset.StandardCharsets

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
        val nftApiError = NftIndexerApiErrorDto(404, NftIndexerApiErrorDto.Code.OWNERSHIP_NOT_FOUND, "message")

        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, nftApiError)

        val ex = assertThrows<ProtocolApiResponseException> {
            nftOrderOwnershipControllerApi
                .getNftOrderOwnershipById(ownershipId.decimalStringValue)
                .block()
        }

        assertThat(ex.responseObject is NftOrderApiErrorDto).isEqualTo(true)
        assertThat((ex.responseObject as NftOrderApiErrorDto).status).isEqualTo(nftApiError.status)
    }

    @Test
    fun `get ownership by id - unexpected api error`() = runBlocking<Unit> {
        val ownershipId = randomOwnershipId()
        val nftApiError = NftIndexerApiErrorDto(500, NftIndexerApiErrorDto.Code.UNKNOWN, "message")

        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, nftApiError)

        val ex = assertThrows<ProtocolApiResponseException> {
            nftOrderOwnershipControllerApi
                .getNftOrderOwnershipById(ownershipId.decimalStringValue)
                .block()
        }

        assertThat(ex.responseObject is NftOrderApiErrorDto).isEqualTo(true)
        assertThat((ex.responseObject as NftOrderApiErrorDto).status).isEqualTo(nftApiError.status)
    }

    @Test
    fun `get ownership by id - unparseable id`() = runBlocking<Unit> {
        val ex = assertThrows<ProtocolApiResponseException> {
            nftOrderOwnershipControllerApi
                .getNftOrderOwnershipById("unparseable value")
                .block()
        }

        val errorData = ex.responseObject
        assertThat(errorData is NftOrderApiErrorDto).isEqualTo(true)
        assertThat((errorData as NftOrderApiErrorDto).status).isEqualTo(500)
        assertThat((errorData).code).isEqualTo(NftOrderApiErrorDto.Code.UNKNOWN)
    }

    @Test
    fun `get ownership by id - not handled response code`() = runBlocking<Unit> {
        val ownershipId = randomOwnershipId()
        val exception = WebClientResponseException.create(
            HttpStatus.CONFLICT.value(),
            "",
            HttpHeaders(),
            ByteArray(0),
            StandardCharsets.UTF_8
        )

        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, exception)

        val ex = assertThrows<WebClientResponseException> {
            nftOrderOwnershipControllerApi
                .getNftOrderOwnershipById(ownershipId.decimalStringValue)
                .block()
        }

        assertThat(ex.rawStatusCode).isEqualTo(HttpStatus.CONFLICT.value())
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