package com.rarible.protocol.nft.migration.service

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.RoyaltyService
import com.rarible.protocol.nft.core.service.item.ItemCreatorService
import com.rarible.protocol.nft.core.service.item.ItemReduceServiceV1
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
import com.rarible.protocol.nft.core.service.ownership.OwnershipService
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00017UnsupportedLazyItems
import io.daonomic.rpc.domain.Binary
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.util.*
import java.util.concurrent.ThreadLocalRandom

@IntegrationTest
class RemoveLazyItemsMigrationTest : AbstractIntegrationTest() {

    private val migration = ChangeLog00017UnsupportedLazyItems()

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var ownershipService: OwnershipService
    @Autowired
    private lateinit var historyRepository: NftItemHistoryRepository
    private val itemCreatorService: ItemCreatorService = mockk()
    private val eventListenerListener: ReduceEventListenerListener = mockk()
    private val skipTokens: ReduceSkipTokens = mockk()
    private val royaltyService: RoyaltyService = mockk()

    @Autowired
    private lateinit var producer: ProtocolNftEventPublisher

    @BeforeEach
    fun setupMocks() {
        every { itemCreatorService.getCreator(any()) } returns Mono.just(createAddress())
        every { eventListenerListener.onOwnershipChanged(any()) } returns Mono.empty()
        every { eventListenerListener.onItemChanged(any()) } returns Mono.empty()
        every { eventListenerListener.onOwnershipDeleted(any()) } returns Mono.empty()
    }

    @Test
    fun `should remove lazy items`() = runBlocking {
        val itemReduceService = ItemReduceServiceV1(itemRepository, ownershipService, historyRepository,
            lazyNftItemHistoryRepository, itemCreatorService, eventListenerListener, skipTokens, royaltyService, FeatureFlags()
        )

        // non lazy collection
        val contract = createToken().copy(
            standard = TokenStandard.ERC721,
            features = setOf(TokenFeature.APPROVE_FOR_ALL, TokenFeature.BURN)
        )
        tokenRepository.save(contract).awaitFirst()

        // lazy item in non lazy collection
        val lazyItem = createItemLazyMint(contract.id)
        val savedItemHistory = lazyNftItemHistoryRepository.save(lazyItem).awaitSingle()
        itemReduceService.update(savedItemHistory.token, savedItemHistory.tokenId).awaitFirstOrNull()
        assertFalse(item(lazyItem).deleted)

        // non lazy item in non lazy collection
        val nonLazyItem = itemRepository.save(createItem()).awaitSingle()
        assertFalse(item(nonLazyItem).deleted)

        // lazy collection
        val lazyContract = createToken().copy(
            standard = TokenStandard.ERC721,
            features = setOf(TokenFeature.APPROVE_FOR_ALL, TokenFeature.BURN, TokenFeature.MINT_AND_TRANSFER)
        )
        tokenRepository.save(lazyContract).awaitFirst()

        // lazy item in lazy collection
        val lazyItem2Lazy = createItemLazyMint(lazyContract.id)
        val savedLazyItem2Lazy = lazyNftItemHistoryRepository.save(lazyItem2Lazy).awaitSingle()
        itemReduceService.update(savedLazyItem2Lazy.token, savedLazyItem2Lazy.tokenId).awaitFirstOrNull()
        assertFalse(item(lazyItem2Lazy).deleted)

        // check ownerships
        assertEquals(2, ownershipCount())
        assertEquals(2, lazyHistoryCount())

        migration.remove(mongo, producer)

        assertTrue(item(lazyItem).deleted)
        assertFalse(item(nonLazyItem).deleted)
        assertFalse(item(lazyItem2Lazy).deleted)
        assertEquals(1, ownershipCount())
        assertEquals(1, lazyHistoryCount())
    }

    private suspend fun item(history: ItemHistory) =
        itemRepository.findById(ItemId(history.token, history.tokenId)).awaitSingle()

    private suspend fun item(item: Item) = itemRepository.findById(item.id).awaitSingle()

    private suspend fun ownershipCount() = mongo.count(Query(), OwnershipRepository.COLLECTION).awaitFirst()

    private suspend fun lazyHistoryCount() = mongo.count(Query(), LazyNftItemHistoryRepository.COLLECTION).awaitFirst()

    private fun createToken(): Token {
        return Token(
            id = AddressFactory.create(),
            owner = AddressFactory.create(),
            name = UUID.randomUUID().toString(),
            symbol = UUID.randomUUID().toString(),
            status = ContractStatus.values().random(),
            features = (1..10).map {  TokenFeature.values().random() }.toSet(),
            standard = TokenStandard.values().random(),
            version = null
        )
    }

    private fun createItemLazyMint(token: Address) = ItemLazyMint(
        token = token,
        tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 2)),
        uri = UUID.randomUUID().toString(),
        standard = listOf(TokenStandard.ERC1155, TokenStandard.ERC721).random(),
        date = nowMillis(),
        value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000)),
        creators = listOf(Part(AddressFactory.create(), 5000), Part(AddressFactory.create(), 5000)),
        signatures = listOf(Binary.empty(), Binary.empty()),
        royalties = listOf(createPart(), createPart())
    )

    private fun createItem(): Item {
        val token = createAddress()
        val tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
        return Item(
            token = token,
            tokenId = tokenId,
            creators = listOf(createPart(), createPart()),
            supply = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000)),
            lazySupply = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000)),
            royalties = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createPart() },
            owners = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createAddress() },
            date = nowMillis()
        )
    }

    private fun createPart(): Part {
        return Part(
            account = createAddress(),
            value = ThreadLocalRandom.current().nextInt(1, 10000)
        )
    }

    private fun createAddress(): Address {
        val bytes = ByteArray(20)
        ThreadLocalRandom.current().nextBytes(bytes)
        return Address.apply(bytes)
    }
}
