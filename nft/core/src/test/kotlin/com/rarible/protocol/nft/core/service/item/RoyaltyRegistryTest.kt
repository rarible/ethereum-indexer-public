package com.rarible.protocol.nft.core.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.royalties.RoyaltiesRegistry
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.Royalty
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.RoyaltyRepository
import com.rarible.protocol.nft.core.service.RoyaltyService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scala.Tuple2
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.time.Instant

@IntegrationTest
@Disabled
class RoyaltyRegistryTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var historyService: ItemReduceService

    @Autowired
    private lateinit var royaltyRepository: RoyaltyRepository

    @Autowired
    private lateinit var royaltyService: RoyaltyService

    @Test
    fun `should get royalty from contract`() = runBlocking {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE

        val userSender = newSender().second
        val royaltyContract = RoyaltiesRegistry.deployAndWait(userSender, poller).awaitFirst()
        nftIndexerProperties.royaltyRegistryAddress = royaltyContract.address().prefixed()
        royaltyContract.__RoyaltiesRegistry_init().execute().verifySuccess()

        val royalty1 = Tuple2.apply(AddressFactory.create(), BigInteger.ONE)
        royaltyContract.setRoyaltiesByTokenAndTokenId(token, tokenId.value, listOf(royalty1).toTypedArray()).execute()
            .verifySuccess()

        //
        tokenRepository.save(Token(token, name = "TEST", standard = TokenStandard.ERC721)).awaitFirst()
        val transfer = ItemTransfer(
            owner = AddressFactory.create(),
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(2)
        )
        saveItemHistory(transfer)

        val owner = AddressFactory.create()
        val transfer2 = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(3)
        )
        saveItemHistory(transfer2)

        historyService.update(token, tokenId).awaitFirstOrNull()

        // check item
        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertEquals(royalty1._1, item.royalties.get(0).account)
        assertEquals(royalty1._2.toInt(), item.royalties.get(0).value)

        // check royalty in the cache
        assertEquals(1, royaltyRepository.count().awaitFirst())
        val royaltyPE = royaltyRepository.findByItemId(ItemId(token, tokenId)).awaitFirstOrNull()
        assertNotNull(royaltyPE)
        assertEquals(royalty1._1, royaltyPE?.royalty?.get(0)?.account)
        assertEquals(royalty1._2.toInt(), royaltyPE?.royalty?.get(0)?.value)
    }

    @Test
    fun `should get royalty from cache`() = runBlocking {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE

        // set royalty
        val royalty1 = Tuple2.apply(AddressFactory.create(), BigInteger.ONE)
        val parts = listOf(Part(royalty1._1, royalty1._2.intValueExact()))
        val record = Royalty(
            address = token,
            tokenId = tokenId,
            royalty = parts
        )
        royaltyRepository.save(record).awaitFirst()

        //
        tokenRepository.save(Token(token, name = "TEST", standard = TokenStandard.ERC721)).awaitFirst()
        val transfer = ItemTransfer(
            owner = AddressFactory.create(),
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(2)
        )
        saveItemHistory(transfer)

        val owner = AddressFactory.create()
        val transfer2 = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(3)
        )
        saveItemHistory(transfer2)

        historyService.update(token, tokenId).awaitFirstOrNull()

        // check item
        val item = itemRepository.findById(ItemId(token, tokenId)).awaitFirst()
        assertEquals(royalty1._1, item.royalties.get(0).account)
        assertEquals(royalty1._2.toInt(), item.royalties.get(0).value)

        // check royalty cache size
        assertEquals(1, royaltyRepository.count().awaitFirst())
    }

    @Test
    fun `should get royalty from lazy mint`() = runBlocking<Unit> {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE

        val itemLazyMint = ItemLazyMint(
            token = token,
            tokenId = tokenId,
            value = EthUInt256.ONE,
            date = Instant.now(),
            uri = "",
            standard = TokenStandard.ERC721,
            creators = listOf(Part(randomAddress(), 1000)),
            royalties = listOf(Part(randomAddress(), 1000)),
            signatures = emptyList()
        )
        lazyNftItemHistoryRepository.save(itemLazyMint).awaitFirstOrNull()

        val royalties = royaltyService.getByToken(token, tokenId)
        assertThat(royalties).isEqualTo(itemLazyMint.royalties)
    }

    // restoring address after tests
    private lateinit var royaltyRegistryAddress: String

    @BeforeEach
    fun remember() {
        royaltyRegistryAddress = nftIndexerProperties.royaltyRegistryAddress
    }
    @AfterEach
    fun cleanup() {
        nftIndexerProperties.royaltyRegistryAddress = royaltyRegistryAddress
    }
}
