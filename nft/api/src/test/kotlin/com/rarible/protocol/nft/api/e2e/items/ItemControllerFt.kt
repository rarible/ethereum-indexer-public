package com.rarible.protocol.nft.api.e2e.items

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.rarible.core.cache.Cache
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.royalties.RoyaltiesRegistry
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createItem
import com.rarible.protocol.nft.api.e2e.data.createItemLazyMint
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.TemporaryItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scala.Tuple2
import scalether.domain.AddressFactory
import scalether.domain.response.TransactionReceipt
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.time.Instant
import java.util.*
import java.util.stream.Stream

@End2EndTest
class ItemControllerFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @Autowired
    private lateinit var temporaryItemPropertiesRepository: TemporaryItemPropertiesRepository

    @Autowired
    protected lateinit var nftIndexerProperties: NftIndexerProperties

    @Autowired
    private lateinit var mapper : ObjectMapper

    companion object {
        @JvmStatic
        fun lazyNft(): Stream<ItemLazyMint> {
            val erc721 = createItemLazyMint().copy(standard = TokenStandard.ERC721)
            val erc1155 = createItemLazyMint().copy(standard = TokenStandard.ERC1155)
            return Stream.of(erc721, erc1155)
        }
    }

    @Test
    @Disabled
    fun `should get item meta`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item).awaitFirst()

        val itemAttributes = listOf(
            ItemAttribute("attr1", "value1"),
            ItemAttribute("attr2", "value2")
        )

        val itemProperties = ItemProperties(
            name = "ItemMetaName",
            description = "ItemMetaDescription",
            image = "http://127.0.0.1/ItemMetaImage.png",
            imagePreview = "http://127.0.0.1/ItemMetaImagePreview.bmp",
            imageBig = "http://t127.0.0.1/ItemMetaImageBig.jpeg",
            animationUrl = "http://127.0.0.1/ItemMetaImageUrl.gif",
            attributes = itemAttributes
        )

        val tempItemProperties = TemporaryItemProperties(
            id = item.id.decimalStringValue,
            value = itemProperties
        )
        temporaryItemPropertiesRepository.save(tempItemProperties).block()

        val metaDto = nftItemApiClient.getNftItemMetaById(item.id.decimalStringValue).awaitFirst()

        assertThat(metaDto.name).isEqualTo(itemProperties.name)
        assertThat(metaDto.description).isEqualTo(itemProperties.description)
        assertThat(metaDto.image!!.url[NftMediaSizeDto.ORIGINAL.name]).isEqualTo(itemProperties.image)
        assertThat(metaDto.image!!.url[NftMediaSizeDto.BIG.name]).isEqualTo(itemProperties.imageBig)
        assertThat(metaDto.image!!.url[NftMediaSizeDto.PREVIEW.name]).isEqualTo(itemProperties.imagePreview)
        assertThat(metaDto.image!!.meta[NftMediaSizeDto.PREVIEW.name]).isEqualTo(null)
        assertThat(metaDto.animation!!.url[NftMediaSizeDto.ORIGINAL.name]).isEqualTo(itemProperties.animationUrl)
        assertThat(metaDto.animation!!.meta[NftMediaSizeDto.ORIGINAL.name]!!.type).isEqualTo("image/gif")
        assertThat(metaDto.attributes!![0].key).isEqualTo(itemProperties.attributes[0].key)
        assertThat(metaDto.attributes!![0].value).isEqualTo(itemProperties.attributes[0].value)
        assertThat(metaDto.attributes!![1].key).isEqualTo(itemProperties.attributes[1].key)
        assertThat(metaDto.attributes!![1].value).isEqualTo(itemProperties.attributes[1].value)
    }

    @Test
    @Disabled // this test use real request ipfs
    fun `should get item meta image`() = runBlocking<Unit> {
        val item = createItem()
        lazyNftItemHistoryRepository.save(ItemLazyMint(
            token = item.token,
            tokenId = item.tokenId,
            creators = listOf(Part(AddressFactory.create(), 1000)),
            value = EthUInt256.ONE,
            date = Instant.now(),
            uri = "/ipfs/QmXDQX1RcE7zkxFE3ah727DZnQBg5wztdBx2br4wsyRrZm",
            standard = TokenStandard.ERC721,
            royalties = listOf(),
            signatures = listOf()
        )).awaitFirstOrNull()

        val metaDto = nftItemApiClient.getNftItemMetaById(item.id.decimalStringValue).awaitSingle()

        assertEquals(NftItemMetaDto(
            name = "test - видео без обложки",
            description = "",
            attributes = listOf(),
            image = null,
            animation = NftMediaDto(
                url = mapOf("ORIGINAL" to "ipfs://ipfs/QmVw7dtKv4r7KxRJouxZZTndWviwFXSjA7QhYDmQESSdFY/animation.mp4"),
                meta = mapOf("ORIGINAL" to NftMediaMetaDto("video/mp4", null, null))
            )
        ), metaDto)
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
        val item = createItem()
        itemRepository.save(item).awaitFirst()

        val itemDto = nftItemApiClient.getNftItemById(item.id.decimalStringValue).awaitFirst()
        assertThat(itemDto.id).isEqualTo(item.id.decimalStringValue)
        assertThat(itemDto.contract).isEqualTo(item.token)
        assertThat(itemDto.tokenId).isEqualTo(item.tokenId.value)
        assertThat(itemDto.supply).isEqualTo(item.supply.value)
        assertThat(itemDto.owners).isEqualTo(item.owners)
        assertThat(itemDto.meta).isNotNull

        assertThat(itemDto.creators.size).isEqualTo(item.creators.size)
        itemDto.creators.forEachIndexed { index, partDto ->
            assertThat(partDto.account).isEqualTo(item.creators.toList()[index].account)
            assertThat(partDto.value).isEqualTo(item.creators.toList()[index].value)
        }

        itemDto.royalties.forEachIndexed { index, royaltyDto ->
            assertThat(royaltyDto.account).isEqualTo(item.royalties[index].account)
            assertThat(royaltyDto.value).isEqualTo(item.royalties[index].value)
        }

        val list = nftItemApiClient.getNftItemsByOwner(item.owners.first().hex(), null, null).awaitFirst()
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
    fun `should get all items by owner`() = runBlocking {
        val owner = AddressFactory.create()
        val item1 = createItem().copy(owners = listOf(owner))
        val item2 = createItem().copy(owners = listOf(owner, AddressFactory.create()))
        val item3 = createItem().copy(owners = listOf(owner))
        val item4 = createItem().copy(owners = listOf(owner, AddressFactory.create()))
        val item5 = createItem().copy(owners = listOf(owner, AddressFactory.create()))

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
        allItems.forEach {
            assertThat(it.meta).isNotNull
        }
    }

    @Test
    fun `should get null continuation`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val item1 = createItem().copy(owners = listOf(owner))

        listOf(item1).forEach { itemRepository.save(it).awaitFirst() }

        val itemsDto = nftItemApiClient.getNftItemsByOwner(owner.hex(), null, 2).awaitFirst()

        assertNull(itemsDto.continuation)
        assertThat(itemsDto.items).hasSize(1)
    }

    @Test
    fun `should get all items with meta`() = runBlocking {
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

        itemsDto.items.forEach {
            assertThat(it.meta).isNotNull
        }
    }

    @Test
    fun `should get royalty by itemId from contract`() = runBlocking {
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
    fun `should get royalty by itemId from cache`() = runBlocking {
        val item = createItem()

        // set royalty
        val royalty = Tuple2.apply(AddressFactory.create(), BigInteger.ONE)
        val cache = Cache(item.id.toString(), listOf(Part(royalty._1, royalty._2.intValueExact())), Date())
        mongo.save(cache, "cache_royalty").awaitSingle()

        // get from api
        val dto = nftItemApiClient.getNftItemRoyaltyById(item.id.toString()).awaitSingle()
        assertEquals(NftItemRoyaltyListDto(listOf(NftItemRoyaltyDto(royalty._1, royalty._2.intValueExact()))), dto)
    }

    @ParameterizedTest
    @MethodSource("lazyNft")
    fun `should get lazy item by id`(itemLazyMint: ItemLazyMint) = runBlocking<Unit> {
        lazyNftItemHistoryRepository.save(itemLazyMint).awaitFirst()

        val lazyItemDto = nftItemApiClient.getNftLazyItemById(ItemId(itemLazyMint.token, itemLazyMint.tokenId).stringValue).awaitFirst()

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

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success())
        return receipt
    }

    protected suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
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
