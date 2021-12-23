package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.TokenId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class TokenIdRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var tokenIdRepository: TokenIdRepository

    @Test
    fun newTokenId() = runBlocking<Unit> {
        val newTokenId = tokenIdRepository.generateTokenId("non-existent")
        assertThat(newTokenId)
            .isEqualTo(1L)
    }

    @Test
    fun existingTokenId() = runBlocking<Unit> {
        mongo.save(TokenId("exists", 10)).awaitFirst()
        val newTokenId = tokenIdRepository.generateTokenId("exists")
        assertThat(newTokenId)
            .isEqualTo(11L)
    }
}
