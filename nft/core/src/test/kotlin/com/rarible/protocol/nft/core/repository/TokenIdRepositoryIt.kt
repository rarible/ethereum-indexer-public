package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.TokenId
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class TokenIdRepositoryIt : AbstractIntegrationTest() {

    private val zeroInitValue = NftIndexerProperties.CollectionProperties(0)
    private val nonZeroInitValue = NftIndexerProperties.CollectionProperties(100)

    @Test
    fun `new token id - 0 initial value`() = runBlocking<Unit> {
        val tokenIdRepository = TokenIdRepository(mongo, zeroInitValue)
        val newTokenId = tokenIdRepository.generateTokenId("non-existent")
        assertThat(newTokenId)
            .isEqualTo(1L)
    }

    @Test
    fun `existing token id - 0 initial value`() = runBlocking<Unit> {
        val tokenIdRepository = TokenIdRepository(mongo, zeroInitValue)
        mongo.save(TokenId("exists", 10)).awaitFirst()
        val newTokenId = tokenIdRepository.generateTokenId("exists")
        assertThat(newTokenId)
            .isEqualTo(11L)
    }

    @Test
    fun `new token id - 100 initial value`() = runBlocking<Unit> {
        val tokenIdRepository = TokenIdRepository(mongo, nonZeroInitValue)
        val newTokenId = tokenIdRepository.generateTokenId("non-existent")
        assertThat(newTokenId)
            .isEqualTo(101L)
    }

    @Test
    fun `existing token id - 100 initial value`() = runBlocking<Unit> {
        val tokenIdRepository = TokenIdRepository(mongo, nonZeroInitValue)
        mongo.save(TokenId("exists", 10)).awaitFirst()
        val newTokenId = tokenIdRepository.generateTokenId("exists")
        // Should not affect existing values
        assertThat(newTokenId)
            .isEqualTo(11L)
    }
}
