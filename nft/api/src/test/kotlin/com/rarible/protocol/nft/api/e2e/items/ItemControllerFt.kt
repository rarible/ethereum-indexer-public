package com.rarible.protocol.nft.api.e2e.items

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createItem
import com.rarible.protocol.nft.api.e2e.data.createItemLazyMint
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.TemporaryItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException
import scalether.domain.AddressFactory
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
            image = "http://test.com/ItemMetaImage.png",
            imagePreview = "http://test.com/ItemMetaImagePreview.bmp",
            imageBig = "http://test.com/ItemMetaImageBig.jpeg",
            animationUrl = "http://test.com/ItemMetaImageUrl.gif",
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
        assertThat(metaDto.image!!.meta[NftMediaSizeDto.PREVIEW.name]!!.type).isEqualTo("text/html") //TODO is it correct?
        assertThat(metaDto.animation!!.url[NftMediaSizeDto.ORIGINAL.name]).isEqualTo(itemProperties.animationUrl)
        assertThat(metaDto.animation!!.meta[NftMediaSizeDto.ORIGINAL.name]!!.type).isEqualTo("image/gif")
        assertThat(metaDto.attributes!![0].key).isEqualTo(itemProperties.attributes[0].key)
        assertThat(metaDto.attributes!![0].value).isEqualTo(itemProperties.attributes[0].value)
        assertThat(metaDto.attributes!![1].key).isEqualTo(itemProperties.attributes[1].key)
        assertThat(metaDto.attributes!![1].value).isEqualTo(itemProperties.attributes[1].value)
    }

    @Test
    fun `should return bad request`() = runBlocking {
        try {
            val result = nftItemApiClient.getNftLazyItemById("-").awaitFirst()
        } catch (ex : WebClientResponseException.BadRequest) {
            val dto = mapper.readValue(ex.responseBodyAsString, NftIndexerApiErrorDto::class.java)
            assertEquals(400, dto.status)
            assertEquals(NftIndexerApiErrorDto.Code.BAD_REQUEST, dto.code)
            assertEquals("Incorrect format of itemId: -", dto.message)
        }
    }

    @Test
    fun `should get item by id`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item).awaitFirst()

        val itemDto = nftItemApiClient.getNftItemById(item.id.decimalStringValue, null).awaitFirst()
        assertThat(itemDto.id).isEqualTo(item.id.decimalStringValue)
        assertThat(itemDto.contract).isEqualTo(item.token)
        assertThat(itemDto.tokenId).isEqualTo(item.tokenId.value)
        assertThat(itemDto.supply).isEqualTo(item.supply.value)
        assertThat(itemDto.owners).isEqualTo(item.owners)
        assertThat(itemDto.meta).isNull()

        assertThat(itemDto.creators.size).isEqualTo(item.creators.size)
        itemDto.creators.forEachIndexed { index, partDto ->
            assertThat(partDto.account).isEqualTo(item.creators.toList()[index].account)
            assertThat(partDto.value).isEqualTo(item.creators.toList()[index].value)
        }

        itemDto.royalties.forEachIndexed { index, royaltyDto ->
            assertThat(royaltyDto.account).isEqualTo(item.royalties[index].account)
            assertThat(royaltyDto.value).isEqualTo(item.royalties[index].value)
        }

        val list = nftItemApiClient.getNftItemsByOwner(item.owners.first().hex(), null, null, null).awaitFirst()
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
    fun `should get item with meta`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item).awaitFirst()

        val itemDto = nftItemApiClient.getNftItemById(item.id.decimalStringValue, true).awaitFirst()
        assertThat(itemDto.id).isEqualTo(item.id.decimalStringValue)
        assertThat(itemDto.meta).isNotNull
    }

    @Test
    fun `should get all items by owner`() = runBlocking<Unit> {
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
            val itemsDto = nftItemApiClient.getNftItemsByOwner(owner.hex(), continuation, 2, false).awaitFirst()
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
            assertThat(it.meta).isNull()
        }
    }

    @Test
    fun `should get null continuation`() = runBlocking<Unit> {
        val owner = AddressFactory.create()
        val item1 = createItem().copy(owners = listOf(owner))

        listOf(item1).forEach { itemRepository.save(it).awaitFirst() }

        val itemsDto = nftItemApiClient.getNftItemsByOwner(owner.hex(), null, 2, false).awaitFirst()

        assertNull(itemsDto.continuation)
        assertThat(itemsDto.items).hasSize(1)
    }

    @Test
    fun `should get all items with meta`() = runBlocking<Unit> {
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

        val itemsDto = nftItemApiClient.getNftItemsByOwner(owner.hex(), null, 10, true).awaitFirst()

        assertThat(itemsDto.items).hasSizeLessThanOrEqualTo(5)

        itemsDto.items.forEach {
            assertThat(it.meta).isNotNull
        }
    }

    @ParameterizedTest
    @MethodSource("lazyNft")
    fun `should get lazy item by id`(itemLazyMint: ItemLazyMint) = runBlocking<Unit> {
        lazyNftItemHistoryRepository.save(itemLazyMint).awaitFirst()

        val lazyItemDto = nftItemApiClient.getNftLazyItemById(itemLazyMint.id).awaitFirst()

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
}
