package com.rarible.protocol.nft.api.e2e.items

import com.rarible.protocol.dto.CollectionsByIdRequestDto
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.api.test.AbstractIntegrationTest
import com.rarible.protocol.nft.api.test.End2EndTest
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import kotlin.random.Random

@End2EndTest
class CollectionControllerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Test
    fun `should get collections by owner`() = runBlocking {
        val owner = AddressFactory.create()
        val token1 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155)
        val token2 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155)
        val token3 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155)
        val token4 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155)

        // Shouldn't get collections with an "ERROR" status
        val token5 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)
        val token6 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)

        listOf(token1, token2, token3, token4, token5, token6, createToken(), createToken(), createToken()).forEach {
            tokenRepository.save(it).awaitFirst()
        }

        val result1 = nftCollectionApiClient.searchNftCollectionsByOwner(owner.hex(), null, 10).awaitFirst()
        assertThat(result1.collections).hasSize(4)
        assertThat(result1.collections.map { it.id })
            .containsExactlyInAnyOrder(token1.id, token2.id, token3.id, token4.id)

        assertThat(result1.continuation).isNull()
    }

    @Test
    fun `shouldn't get collections by owner with an error status`() = runBlocking {
        val owner = AddressFactory.create()
        val token1 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)
        val token2 = createToken().copy(owner = owner, standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)

        listOf(token1, token2).forEach {
            tokenRepository.save(it).awaitFirst()
        }

        val result1 = nftCollectionApiClient.searchNftCollectionsByOwner(owner.hex(), null, 10).awaitFirst()
        assertThat(result1.collections).hasSize(0)

        assertThat(result1.continuation).isNull()
    }

    @Test
    fun `should get all collections`() = runBlocking {
        val token1 = createToken().copy(standard = TokenStandard.ERC1155)
        val token2 = createToken().copy(standard = TokenStandard.ERC1155)
        val token3 = createToken().copy(standard = TokenStandard.ERC1155)
        val token4 = createToken().copy(standard = TokenStandard.ERC1155)

        // Shouldn't get collections with an "ERROR" status
        val token5 = createToken().copy(standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)
        val token6 = createToken().copy(standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)

        listOf(token1, token2, token3, token4, token5, token6).forEach {
            tokenRepository.save(it).awaitFirst()
        }

        val result1 = nftCollectionApiClient.searchNftAllCollections(null, 10).awaitFirst()
        assertThat(result1.collections).hasSizeGreaterThanOrEqualTo(4)
        assertThat(result1.collections.map { it.id }).contains(token1.id, token2.id, token3.id, token4.id)

        assertThat(result1.continuation).isNull()
    }

    @Test
    fun `shouldn't get collections with an error status`() = runBlocking {
        val token1 = createToken().copy(standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)
        val token2 = createToken().copy(standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)

        listOf(token1, token2).forEach {
            tokenRepository.save(it).awaitFirst()
        }

        val result1 = nftCollectionApiClient.searchNftAllCollections(null, 10).awaitFirst()
        assertThat(result1.collections).hasSizeGreaterThanOrEqualTo(0)

        assertThat(result1.continuation).isNull()
    }

    @Test
    fun `should return collections by ids`() = runBlocking {
        val token1 = createToken().copy(standard = TokenStandard.ERC1155)
        val token2 = createToken().copy(standard = TokenStandard.ERC1155)
        val token3 = createToken().copy(standard = TokenStandard.ERC1155)
        val token4 = createToken().copy(standard = TokenStandard.ERC1155)
        val token5 = createToken().copy(standard = TokenStandard.ERC1155)

        // collections with an "ERROR" status
        val token6 = createToken().copy(standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)
        val token7 = createToken().copy(standard = TokenStandard.ERC1155, status = ContractStatus.ERROR)

        val tokens = listOf(token1, token2, token3, token4, token5, token6, token7)
        tokens.forEach {
            tokenRepository.save(it).awaitFirst()
        }

        val random = Random.nextInt(3, 7)

        val expectedIds = tokens.take(random).map { it.id }

        val actual = nftCollectionApiClient.getNftCollectionsByIds(CollectionsByIdRequestDto(expectedIds.map { "$it" }))
            .awaitSingle()

        assertThat(actual.collections.map { it.id }).containsAll(expectedIds)
        assertThat(actual.continuation).isNull()
    }
}
