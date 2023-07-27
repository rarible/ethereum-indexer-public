package com.rarible.protocol.nft.core.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFilter
import com.rarible.protocol.nft.core.model.TokenStandard
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

@IntegrationTest
class TokenRepositoryIntegrationTest : AbstractIntegrationTest() {

    @Test
    fun `should search with continuation in id asc order`() = runBlocking<Unit> {
        // given
        val now = nowMillis()
        val three = tokenRepository.save(buildToken(Address.THREE())).awaitFirst().copy(dbUpdatedAt = now)
        val two = tokenRepository.save(buildToken(Address.TWO())).awaitFirst().copy(dbUpdatedAt = now)
        val four = tokenRepository.save(buildToken(Address.FOUR())).awaitFirst().copy(dbUpdatedAt = now)
        val one = tokenRepository.save(buildToken(Address.ONE())).awaitFirst().copy(dbUpdatedAt = now)
        val zero = tokenRepository.save(buildToken(Address.ZERO())).awaitFirst().copy(dbUpdatedAt = now)
        var filter = TokenFilter.All(null, 2)

        // when
        val batch1 = tokenRepository.search(filter).collectList().awaitFirst()
            .map { it.copy(dbUpdatedAt = now) }
        filter = filter.copy(batch1.last().id.toString())
        val batch2 = tokenRepository.search(filter).collectList().awaitFirst()
            .map { it.copy(dbUpdatedAt = now) }
        filter = filter.copy(batch2.last().id.toString())
        val batch3 = tokenRepository.search(filter).collectList().awaitFirst()
            .map { it.copy(dbUpdatedAt = now) }

        // then
        assertThat(batch1).containsExactly(zero, one)
        assertThat(batch2).containsExactly(two, three)
        assertThat(batch3).containsExactly(four)
    }

    private fun buildToken(id: Address): Token {
        return Token(
            id = id,
            name = id.toString(),
            standard = TokenStandard.ERC721,
        )
    }
}
