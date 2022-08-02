package com.rarible.protocol.nft.api.e2e.items

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.rarible.core.cache.Cache
import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.royalties.RoyaltiesRegistry
import com.rarible.protocol.dto.EthereumApiErrorBadRequestDto
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemIdsDto
import com.rarible.protocol.dto.NftItemRoyaltyDto
import com.rarible.protocol.dto.NftItemRoyaltyListDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createItem
import com.rarible.protocol.nft.api.e2e.data.createItemLazyMint
import com.rarible.protocol.nft.api.e2e.data.createOwnership
import com.rarible.protocol.nft.api.e2e.data.createPart
import com.rarible.protocol.nft.api.e2e.data.randomItemMeta
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.dto.ItemDtoConverter
import com.rarible.protocol.nft.core.converters.dto.NftItemMetaDtoConverter
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import io.daonomic.rpc.domain.Binary
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scala.Tuple2
import scalether.domain.AddressFactory
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.util.Date
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Stream

@End2EndTest
class ItemControllerFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @Autowired
    protected lateinit var nftIndexerProperties: NftIndexerProperties

    @Autowired
    protected lateinit var featureFlags: FeatureFlags

    @Autowired
    private lateinit var nftItemMetaDtoConverter: NftItemMetaDtoConverter

    private lateinit var itemDtoConverter: ItemDtoConverter

    @BeforeEach
    fun beforeEach() {
        itemDtoConverter = ItemDtoConverter()
    }

    companion object {

        @JvmStatic
        fun lazyNft(): Stream<ItemLazyMint> {
            val erc721 = createItemLazyMint().copy(standard = TokenStandard.ERC721)
            val erc1155 = createItemLazyMint().copy(standard = TokenStandard.ERC1155)
            return Stream.of(erc721, erc1155)
        }
    }

    @Test
    fun `should get item meta - success`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item).awaitFirst()
        val itemMeta = randomItemMeta()
        coEvery { mockItemMetaResolver.resolveItemMeta(item.id) } returns itemMeta
        val metaDto = nftItemApiClient.getNftItemMetaById(item.id.decimalStringValue).awaitFirst()
        assertThat(metaDto).isEqualTo(nftItemMetaDtoConverter.convert(itemMeta, item.id.decimalStringValue))
    }

    @Test
    fun `should get item meta - return 404 on not found meta`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item).awaitFirst()
        coEvery { mockItemMetaResolver.resolveItemMeta(item.id) } returns null
        assertThrows<NftItemControllerApi.ErrorGetNftItemMetaById> {
            nftItemApiClient.getNftItemMetaById(item.id.decimalStringValue).awaitFirst()
        }
    }

    @Test
    fun `should get 400 on wrong address param`() = runBlocking<Unit> {
        val error = assertThrows<NftItemControllerApi.ErrorGetNftItemsByOwner> {
            nftItemApiClient.getNftItemsByOwner("invalid", null, null).awaitFirst()
        }
        assertThat(error.on400).isNotNull
    }

    @Test
    fun `should get item meta - return 404 if meta cannot be loaded`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item).awaitFirst()
        coEvery { mockItemMetaResolver.resolveItemMeta(item.id) } returns null
        assertThrows<NftItemControllerApi.ErrorGetNftItemMetaById> {
            nftItemApiClient.getNftItemMetaById(item.id.decimalStringValue).awaitFirst()
        }
    }

    @Test
    fun `should return bad request`() = runBlocking<Unit> {
        try {
            nftItemApiClient.getNftLazyItemById("-").awaitFirst()
        } catch (ex: NftItemControllerApi.ErrorGetNftLazyItemById) {
            val dto = ex.on400
            assertEquals(400, ex.rawStatusCode)
            assertEquals(EthereumApiErrorBadRequestDto.Code.BAD_REQUEST, dto.code)
        }
    }

    @Test
    fun `should get item by id`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val item = createItem().copy(owners = listOf(owner))
        itemRepository.save(item).awaitFirst()
        val ownership = createOwnership(item.token, item.tokenId, null, owner)
        ownershipRepository.save(ownership).awaitFirst()

        // On the first request, item meta is null (because it was not loaded before). Loading gets scheduled.
        assertThat(nftItemApiClient.getNftItemById(item.id.decimalStringValue).awaitSingle())
            .isEqualTo(itemDtoConverter.convert(item))

        // Then the meta gets loaded. Wait for it.
        Wait.waitAssert {
            assertThat(nftItemApiClient.getNftItemById(item.id.decimalStringValue).awaitSingle())
                .isEqualTo(itemDtoConverter.convert(item))
        }
        val itemDto = nftItemApiClient.getNftItemById(item.id.decimalStringValue).awaitSingle()

        assertThat(itemDto.creators.size).isEqualTo(item.creators.size)
        itemDto.creators.forEachIndexed { index, partDto ->
            assertThat(partDto.account).isEqualTo(item.creators.toList()[index].account)
            assertThat(partDto.value).isEqualTo(item.creators.toList()[index].value)
        }

        itemDto?.royalties?.forEachIndexed { index, royaltyDto ->
            assertThat(royaltyDto.account).isEqualTo(item.royalties[index].account)
            assertThat(royaltyDto.value).isEqualTo(item.royalties[index].value)
        }

        val list = nftItemApiClient.getNftItemsByOwner(owner.hex(), null, null).awaitFirst()
        assertThat(list.items)
            .hasSize(1)
        assertThat(list.items.firstOrNull())
            .hasFieldOrPropertyWithValue("id", item.id.decimalStringValue)

        val body = webClient.get().uri("/v0.1/items/{id}", item.id.decimalStringValue).retrieve()
            .bodyToMono(ObjectNode::class.java).awaitFirst()
        assertThat(body.get("tokenId"))
            .isOfAnyClassIn(TextNode::class.java)
    }

    @Test
    fun `should get all items by owner`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val item1 = createItem()
        val item2 = createItem()
        val item3 = createItem()
        val item4 = createItem()
        val item5 = createItem()

        listOf(
            createItem(),
            item1,
            item2,
            item3,
            createItem(),
            item4,
            item5,
            createItem()
        ).forEach { itemRepository.save(it).awaitFirst() }

        listOf(item1, item2, item3, item4, item5)
            .map { createOwnership(it.token, it.tokenId, null, owner) }
            .forEach { ownershipRepository.save(it).awaitFirst() }

        val allItems = mutableListOf<NftItemDto>()
        var continuation: String? = null
        do {
            val itemsDto = nftItemApiClient.getNftItemsByOwner(owner.hex(), continuation, 2).awaitFirst()
            assertThat(itemsDto.items).hasSizeLessThanOrEqualTo(2)

            allItems.addAll(itemsDto.items)
            continuation = itemsDto.continuation
        } while (continuation != null)

        assertThat(allItems).hasSize(5)

        assertThat(allItems.map { it.id }).containsExactlyInAnyOrder(
            item1.id.decimalStringValue,
            item2.id.decimalStringValue,
            item3.id.decimalStringValue,
            item4.id.decimalStringValue,
            item5.id.decimalStringValue
        )
    }

    @Test
    fun `should get item by collection`() = runBlocking<Unit> {
        val token = AddressFactory.create()
        val item = createItem().copy(token = token)

        listOf(
            createItem(),
            item,
            createItem()
        ).forEach { itemRepository.save(it).awaitFirst() }

        val allItems = mutableListOf<NftItemDto>()
        var continuation: String? = null
        do {
            val itemsDto = nftItemApiClient.getNftItemsByCollection(token.hex(), null, continuation, 2).awaitFirst()
            assertThat(itemsDto.items).hasSizeLessThanOrEqualTo(2)

            allItems.addAll(itemsDto.items)
            continuation = itemsDto.continuation
        } while (continuation != null)

        assertThat(allItems).hasSize(1)

        assertThat(allItems.map { it.id }).containsExactlyInAnyOrder(item.id.decimalStringValue)
    }

    @Test
    fun `should get item by collection & owner`() = runBlocking<Unit> {
        val token = AddressFactory.create()
        val owner = AddressFactory.create()
        val item = createItem().copy(token = token, owners = listOf(owner))

        listOf(
            createItem().copy(token = token),
            item,
            createItem().copy(token = token)
        ).forEach { itemRepository.save(it).awaitFirst() }

        val allItems = mutableListOf<NftItemDto>()
        var continuation: String? = null
        do {
            val itemsDto =
                nftItemApiClient.getNftItemsByCollection(token.hex(), owner.hex(), continuation, 2).awaitFirst()
            assertThat(itemsDto.items).hasSizeLessThanOrEqualTo(2)

            allItems.addAll(itemsDto.items)
            continuation = itemsDto.continuation
        } while (continuation != null)

        assertThat(allItems).hasSize(1)

        assertThat(allItems.map { it.id }).containsExactlyInAnyOrder(item.id.decimalStringValue)
    }

    @Test
    fun `should get null continuation`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val item = createItem()
        itemRepository.save(item).awaitFirst()
        val ownership = createOwnership(item.token, item.tokenId, null, owner)
        ownershipRepository.save(ownership).awaitFirst()

        val itemsDto = nftItemApiClient.getNftItemsByOwner(owner.hex(), null, 2).awaitFirst()

        assertNull(itemsDto.continuation)
        assertThat(itemsDto.items).hasSize(1)
    }

    @Test
    fun `should get items by owners`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val item1 = createItem().copy(owners = listOf(owner))
        val item2 = createItem().copy(owners = listOf(owner))
        val item3 = createItem().copy(owners = listOf(owner))
        val item4 = createItem().copy(owners = listOf(owner))
        val item5 = createItem().copy(owners = listOf(owner))

        listOf(
            item1,
            item2,
            item3,
            item4,
            item5
        ).forEach { itemRepository.save(it).awaitFirst() }

        val itemsDto = nftItemApiClient.getNftItemsByOwner(owner.hex(), null, 10).awaitFirst()

        assertThat(itemsDto.items).hasSizeLessThanOrEqualTo(5)
    }

    @Test
    fun `should get royalty by itemId from contract`() = runBlocking<Unit> {
        val item = createItem()

        // set royalty
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        val royaltyContract = RoyaltiesRegistry.deployAndWait(userSender, poller).awaitFirst()
        nftIndexerProperties.royaltyRegistryAddress = royaltyContract.address().prefixed()
        royaltyContract.__RoyaltiesRegistry_init().execute().verifySuccess()

        val royalty = Tuple2.apply(AddressFactory.create(), BigInteger.ONE)
        royaltyContract
            .setRoyaltiesByTokenAndTokenId(item.token, item.tokenId.value, listOf(royalty).toTypedArray())
            .execute().verifySuccess()

        // get from api
        val dto = nftItemApiClient.getNftItemRoyaltyById(item.id.toString()).awaitSingle()
        assertEquals(NftItemRoyaltyListDto(listOf(NftItemRoyaltyDto(royalty._1, royalty._2.intValueExact()))), dto)

        // check cache
        val cache = mongo.findById(item.id.toString(), Cache::class.java, "cache_royalty").awaitSingle()
        assertEquals(listOf(Part(royalty._1, royalty._2.intValueExact())), cache.data)
    }

    @Test
    fun `should get royalty by itemId from cache`() = runBlocking<Unit> {
        val item = createItem()

        // set royalty
        val royalty = Tuple2.apply(AddressFactory.create(), BigInteger.ONE)
        val cache = Cache(item.id.toString(), listOf(Part(royalty._1, royalty._2.intValueExact())), Date())
        mongo.save(cache, "cache_royalty").awaitSingle()

        // get from api
        val dto = nftItemApiClient.getNftItemRoyaltyById(item.id.toString()).awaitSingle()
        assertEquals(NftItemRoyaltyListDto(listOf(NftItemRoyaltyDto(royalty._1, royalty._2.intValueExact()))), dto)
    }

    @Test
    fun `should get royalty by itemId from cache duplicate mint`() = runBlocking<Unit> {
        val item = createItem()
        val itemLazyMint1 = ItemLazyMint(
            token = item.token,
            tokenId = item.tokenId,
            uri = UUID.randomUUID().toString(),
            standard = listOf(TokenStandard.ERC1155, TokenStandard.ERC721).random(),
            date = nowMillis(),
            value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000)),
            creators = listOf(Part(AddressFactory.create(), 5000), Part(AddressFactory.create(), 5000)),
            signatures = listOf(Binary.empty(), Binary.empty()),
            royalties = listOf(createPart())
        )
        val itemLazyMint2 = ItemLazyMint(
            token = item.token,
            tokenId = item.tokenId,
            uri = UUID.randomUUID().toString(),
            standard = listOf(TokenStandard.ERC1155, TokenStandard.ERC721).random(),
            date = nowMillis(),
            value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000)),
            creators = listOf(Part(AddressFactory.create(), 5000), Part(AddressFactory.create(), 5000)),
            signatures = listOf(Binary.empty(), Binary.empty()),
            royalties = listOf(createPart())
        )
        lazyNftItemHistoryRepository.save(itemLazyMint1).awaitFirst()
        lazyNftItemHistoryRepository.save(itemLazyMint2).awaitFirst()
        // get from api
        val dto = nftItemApiClient.getNftItemRoyaltyById(item.id.toString()).awaitSingle()
        assertEquals(
            NftItemRoyaltyListDto(
                listOf(
                    NftItemRoyaltyDto(
                        itemLazyMint2.royalties[0].account,
                        itemLazyMint2.royalties[0].value
                    )
                )
            ), dto
        )
    }

    @Test
    fun `should get items by ids`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val item = createItem().copy(owners = listOf(owner))
        val itemMeta = randomItemMeta()
        coEvery { mockItemMetaResolver.resolveItemMeta(item.id) } returns itemMeta
        itemRepository.save(item).awaitFirst()
        val ownership = createOwnership(item.token, item.tokenId, null, owner)
        ownershipRepository.save(ownership).awaitFirst()

        suspend fun fetchItems(itemId: ItemId) =
            nftItemApiClient.getNftItemsByIds(
                NftItemIdsDto(listOf(itemId.decimalStringValue))
            ).collectList().awaitFirst()

        assertThat(fetchItems(item.id))
            .isEqualTo(listOf(itemDtoConverter.convert(item)))
    }

    @ParameterizedTest
    @MethodSource("lazyNft")
    fun `should get lazy item by id`(itemLazyMint: ItemLazyMint) = runBlocking<Unit> {
        // TODO[meta]: add a test for meta of a lazy item.
        lazyNftItemHistoryRepository.save(itemLazyMint).awaitFirst()

        val lazyItemDto =
            nftItemApiClient.getNftLazyItemById(ItemId(itemLazyMint.token, itemLazyMint.tokenId).stringValue)
                .awaitFirst()

        assertThat(lazyItemDto.contract).isEqualTo(itemLazyMint.token)
        assertThat(lazyItemDto.tokenId).isEqualTo(itemLazyMint.tokenId.value)
        assertThat(lazyItemDto.signatures).isEqualTo(itemLazyMint.signatures)

        lazyItemDto.creators.forEachIndexed { index, it ->
            assertThat(it.account).isEqualTo(itemLazyMint.creators[index].account)
            assertThat(it.value).isEqualTo(itemLazyMint.creators[index].value)
        }

        lazyItemDto.royalties.forEachIndexed { index, it ->
            assertThat(it.account).isEqualTo(itemLazyMint.royalties[index].account)
            assertThat(it.value).isEqualTo(itemLazyMint.royalties[index].value)
        }
        when (itemLazyMint.standard) {
            TokenStandard.ERC721 -> assertThat(lazyItemDto).isInstanceOf(LazyErc721Dto::class.java)
            TokenStandard.ERC1155 -> {
                assertThat(lazyItemDto).isInstanceOf(LazyErc1155Dto::class.java)
                assertThat((lazyItemDto as LazyErc1155Dto).supply).isEqualTo(itemLazyMint.value.value)
            }
            else -> throw IllegalArgumentException("Unexpected token standard ${itemLazyMint.standard}")
        }
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
