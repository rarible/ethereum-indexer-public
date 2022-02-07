package com.rarible.protocol.nft.api.e2e.items

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.rarible.core.cache.Cache
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.royalties.RoyaltiesRegistry
import com.rarible.protocol.dto.EthereumApiErrorBadRequestDto
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemIdsDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemRoyaltyDto
import com.rarible.protocol.dto.NftItemRoyaltyListDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.NftMediaSizeDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createItem
import com.rarible.protocol.nft.api.e2e.data.createItemLazyMint
import com.rarible.protocol.nft.api.e2e.data.createOwnership
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.PendingLogItemProperties
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.PendingLogItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scala.Tuple2
import scalether.domain.AddressFactory
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
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @Autowired
    private lateinit var pendingLogItemPropertiesRepository: PendingLogItemPropertiesRepository

    @Autowired
    protected lateinit var nftIndexerProperties: NftIndexerProperties

    @Autowired
    protected lateinit var featureFlags: FeatureFlags

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var testTemplate: RestTemplate

    private fun baseUrl(): String {
        return "http://localhost:${port}/v0.1"
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
            attributes = itemAttributes,
            rawJsonContent = null
        )

        val tempItemProperties = PendingLogItemProperties(id = item.id.decimalStringValue, value = itemProperties)
        pendingLogItemPropertiesRepository.save(tempItemProperties).block()

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
    fun `get item image`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item).awaitFirst()

        val base64str = randomString()

        val itemProperties = ItemProperties(
            name = "name",
            description = "description",
            image = "http://test.com/abc_original",
            imagePreview = null,
            imageBig = "https://test.com//data:image/png;base64," + Base64.encodeBase64String(base64str.toByteArray()),
            animationUrl = null,
            attributes = emptyList(),
            rawJsonContent = null
        )

        val cache = Cache(
            item.id.decimalStringValue,
            ItemPropertiesService.CachedItemProperties(itemProperties, nowMillis(), true),
            Date()
        )
        mongo.save(cache, "item_metadata").awaitSingle()

        val url = "${baseUrl()}/items/${item.id.decimalStringValue}/image?size="

        val original = testTemplate.getForEntity("${url}ORIGINAL", ByteArray::class.java)

        // Regular URL specified, redirected
        assertThat(original.statusCode).isEqualTo(HttpStatus.FOUND)
        assertThat(original.headers.getFirst(HttpHeaders.LOCATION)).isEqualTo(itemProperties.image)

        // Found Base64 value for url, returned as byteArray with specified content-type
        val big = testTemplate.getForEntity("${url}BIG&hash=2384723984", ByteArray::class.java)
        assertThat(big.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(big.headers.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("image/png")
        assertThat(String(big.body!!)).isEqualTo(base64str)

        // Not found since this link is not specified in meta
        assertThrows<HttpClientErrorException.NotFound> {
            testTemplate.getForEntity("${url}PREVIEW", ByteArray::class.java)
        }
    }

    @Test
    @Disabled // this test use real request ipfs
    fun `should get item meta image`() = runBlocking<Unit> {
        val item = createItem()
        lazyNftItemHistoryRepository.save(
            ItemLazyMint(
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
        val owner = AddressFactory.create()
        val item = createItem().copy(owners = listOf(owner))
        itemRepository.save(item).awaitFirst()
        val ownership = createOwnership(item.token, item.tokenId, null, owner)
        ownershipRepository.save(ownership).awaitFirst()

        val itemDto = nftItemApiClient.getNftItemById(item.id.decimalStringValue).awaitFirst()
        checkDto(itemDto, item)

        when (featureFlags.reduceVersion) {
            ReduceVersion.V1 -> {
                assertThat(itemDto.owners).isEqualTo(item.owners)
            }
            ReduceVersion.V2 -> {
                assertThat(itemDto.owners).containsExactlyElementsOf(item.ownerships.keys)
            }
        }

        assertThat(itemDto.creators.size).isEqualTo(item.creators.size)
        itemDto.creators.forEachIndexed { index, partDto ->
            assertThat(partDto.account).isEqualTo(item.creators.toList()[index].account)
            assertThat(partDto.value).isEqualTo(item.creators.toList()[index].value)
        }

        itemDto.royalties.forEachIndexed { index, royaltyDto ->
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
    fun `should get all items by owner`() = runBlocking {
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

        listOf(item1,  item2, item3, item4, item5)
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
        allItems.forEach {
            assertThat(it.meta).isNotNull
        }
    }

    @Test
    fun `should get item by collection`() = runBlocking {
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
        allItems.forEach {
            assertThat(it.meta).isNotNull
        }
    }

    @Test
    fun `should get item by collection & owner`() = runBlocking {
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
            val itemsDto = nftItemApiClient.getNftItemsByCollection(token.hex(), owner.hex(), continuation, 2).awaitFirst()
            assertThat(itemsDto.items).hasSizeLessThanOrEqualTo(2)

            allItems.addAll(itemsDto.items)
            continuation = itemsDto.continuation
        } while (continuation != null)

        assertThat(allItems).hasSize(1)

        assertThat(allItems.map { it.id }).containsExactlyInAnyOrder(item.id.decimalStringValue)
        allItems.forEach {
            assertThat(it.meta).isNotNull
        }
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

    @Test
    fun `should get item by ids`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item).awaitFirst()

        val items = nftItemApiClient.getNftItemsByIds(NftItemIdsDto(listOf(item.id.decimalStringValue))).collectList().awaitFirst()
        assertThat(items).hasSize(1)
        checkDto(items[0], item)
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

    fun checkDto(itemDto: NftItemDto, item: Item) {
        assertThat(itemDto.id).isEqualTo(item.id.decimalStringValue)
        assertThat(itemDto.contract).isEqualTo(item.token)
        assertThat(itemDto.tokenId).isEqualTo(item.tokenId.value)
        assertThat(itemDto.supply).isEqualTo(item.supply.value)
        assertThat(itemDto.meta).isNotNull
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
