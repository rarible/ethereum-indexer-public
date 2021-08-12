package com.rarible.protocol.nftorder.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.dto.NftTokenIdDto
import com.rarible.protocol.nftorder.api.client.NftOrderCollectionControllerApi
import com.rarible.protocol.nftorder.api.test.AbstractFunctionalTest
import com.rarible.protocol.nftorder.api.test.FunctionalTest
import com.rarible.protocol.nftorder.listener.test.mock.data.randomNftCollectionDto
import com.rarible.protocol.nftorder.listener.test.mock.data.randomNftSignatureDto
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FunctionalTest
class CollectionControllerFt : AbstractFunctionalTest() {

    @Autowired
    lateinit var nftOrderCollectionControllerApi: NftOrderCollectionControllerApi

    @Test
    fun `generate token id`() = runBlocking<Unit> {
        val collection = randomAddress().hex()
        val minter = randomAddress().hex()
        val nftTokenId = NftTokenIdDto(randomBigInt(), randomNftSignatureDto())

        nftCollectionControllerApiMock.mockGenerateTokenId(collection, minter, nftTokenId)

        val result = nftOrderCollectionControllerApi.generateNftOrderTokenId(collection, minter)
            .awaitFirst()

        assertThat(result).isEqualTo(nftTokenId)
    }

    @Test
    fun `get collection by id`() = runBlocking<Unit> {
        val nftCollection = randomNftCollectionDto()

        nftCollectionControllerApiMock.mockGetCollectionById(nftCollection.id.hex(), nftCollection)

        val result = nftOrderCollectionControllerApi.getNftOrderCollectionById(nftCollection.id.hex())
            .awaitFirst()

        assertThat(result).isEqualTo(nftCollection)
    }

    @Test
    fun `search collections by owner`() = runBlocking<Unit> {
        val owner = randomAddress()
        val continuation = "${randomLong()} _ ${randomAddress().hex()}"
        val size = 20
        val nftCollections = listOf(randomNftCollectionDto(), randomNftCollectionDto())

        nftCollectionControllerApiMock.mockSearchCollectionsByOwner(
            owner, continuation, size, nftCollections[0], nftCollections[1]
        )

        val result = nftOrderCollectionControllerApi.searchNftOrderCollectionsByOwner(owner.hex(), continuation, size)
            .awaitFirst()

        assertThat(result.collections).isEqualTo(nftCollections)
    }

    @Test
    fun `search all collections`() = runBlocking<Unit> {
        val continuation = "${randomLong()} _ ${randomAddress().hex()}"
        val size = 10
        val nftCollections = listOf(randomNftCollectionDto(), randomNftCollectionDto())

        nftCollectionControllerApiMock.mockSearchAllCollections(
            continuation, size, nftCollections[0], nftCollections[1]
        )

        val result = nftOrderCollectionControllerApi.searchNftOrderAllCollections(continuation, size)
            .awaitFirst()

        assertThat(result.collections).isEqualTo(nftCollections)
    }

}