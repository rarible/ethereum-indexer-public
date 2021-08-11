package com.rarible.protocol.nft.api.e2e.items

import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory

@End2EndTest
class CollectionControllerTest : SpringContainerBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Test
    fun `should get collections by owner`() = runBlocking {
        val owner = AddressFactory.create()
        val token1 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155)
        val token2 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155)
        val token3 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155)
        val token4 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155)

        listOf(token1, token2, token3, token4, createToken(), createToken(), createToken()).forEach {
            tokenRepository.save(it).awaitFirst()
        }

        val result1 = nftCollectionApiClient.searchNftCollectionsByOwner(owner.hex(), null, 10).awaitFirst()
        assertThat(result1.collections).hasSize(4)
        assertThat(result1.collections.map { it.id })
            .containsExactlyInAnyOrder(token1.id, token2.id, token3.id, token4.id)

        assertThat(result1.continuation).isNull()
    }

    @Test
    fun `should get all collections`() = runBlocking {
        val token1 = createToken().copy(standard = TokenStandard.ERC1155)
        val token2 = createToken().copy(standard = TokenStandard.ERC1155)
        val token3 = createToken().copy(standard = TokenStandard.ERC1155)
        val token4 = createToken().copy(standard = TokenStandard.ERC1155)

        listOf(token1, token2, token3, token4).forEach {
            tokenRepository.save(it).awaitFirst()
        }

        val result1 = nftCollectionApiClient.searchNftAllCollections(null, 10).awaitFirst()
        assertThat(result1.collections).hasSizeGreaterThanOrEqualTo(4)
        assertThat(result1.collections.map { it.id }).contains(token1.id, token2.id, token3.id, token4.id)

        assertThat(result1.continuation).isNull()
    }
}
