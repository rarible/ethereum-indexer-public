package com.rarible.protocol.nft.api.e2e.items

import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createItem
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.api.e2e.data.randomItemMeta
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.time.Duration

@End2EndTest
class CollectionControllerTest : SpringContainerBaseTest() {

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

    @Test
    fun `refresh collection metadata`() = runBlocking<Unit> {
        val token = createToken().copy(standard = TokenStandard.ERC721)
        tokenRepository.save(token).awaitFirst()

        val items = (0 until 10).map {
            createItem().copy(
                token = token.id,
                tokenId = EthUInt256.of(it)
            )
        }
        items.forEach { itemRepository.save(it).awaitFirst() }

        val itemMeta = randomItemMeta()
        coEvery { mockItemMetaResolver.resolveItemMeta(any()) } returns itemMeta

        nftCollectionApiClient.resetNftCollectionMetaById(token.id.prefixed()).awaitFirstOrNull()

        Wait.waitAssert(timeout = Duration.ofSeconds(10)) {
            items.map { it.id }.forEach { itemId ->
                coVerify(exactly = 1) { mockItemMetaResolver.resolveItemMeta(itemId) }
            }
        }

        // TODO[meta]: also in this test make sure the collection metadata is re-loaded.
    }

}
