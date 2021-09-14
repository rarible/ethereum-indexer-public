package com.rarible.protocol.gateway.route

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.*
import com.rarible.protocol.gateway.AbstractIntegrationTest
import com.rarible.protocol.gateway.End2EndTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import scalether.domain.Address
import java.math.BigInteger

@End2EndTest
@Disabled
internal class ProtocolRouteLocatorTest : AbstractIntegrationTest() {
    @Autowired
    @Qualifier("mockNftServerClient")
    private lateinit var mockNftServerClient: MockServerClient

    @Autowired
    @Qualifier("mockNftOrderServerClient")
    private lateinit var mockNftOrderServerClient: MockServerClient

    @Test
    fun `should go to nft service for token generation`() = runBlocking<Unit> {
        val collection = Address.FOUR()
        val minter = Address.ONE()

        val tokenIdDto = NftTokenIdDto(
            tokenId = BigInteger.TEN,
            signature = NftSignatureDto(
                r = Binary.apply("0x12"),
                s = Binary.apply("0x13"),
                v = Byte.MIN_VALUE
            )
        )
        mockNftServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v0.1/collections/${collection.hex()}/generate_token_id")
                    .withQueryStringParameter("minter", minter.hex())
            )
            .respond(
                HttpResponse.response()
                    .withContentType(MediaType.JSON_UTF_8)
                    .withBody(mapper.writeValueAsBytes(tokenIdDto))
            )

        val result = nftCollectionApi.generateNftTokenId(collection.hex(), minter.hex()).awaitFirst()
        assertThat(result).isEqualTo(tokenIdDto)
    }

    @Test
    fun `should go to nft service for item meta`() = runBlocking<Unit> {
        val collection = Address.FOUR()
        val tokenId = BigInteger.TEN
        val id = "$collection:$tokenId"

        val itemMetaDto = createItemMeta()

        mockNftServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v0.1/items/${id.replace(":","%3A")}/meta")
            )
            .respond(
                HttpResponse.response()
                    .withContentType(MediaType.JSON_UTF_8)
                    .withBody(mapper.writeValueAsBytes(itemMetaDto))
            )

        val result = nftItemApi.getNftItemMetaById(id).awaitFirst()
        assertThat(result).isEqualTo(itemMetaDto)
    }

    private fun createItemMeta(): NftItemMetaDto {
        return NftItemMetaDto(
            name = "Test",
            description = null,
            attributes = null,
            image = null,
            animation = null
        )
    }

    @Test
    fun `should go to nft service for item lazy`() = runBlocking<Unit> {
        val collection = Address.FOUR()
        val tokenId = BigInteger.TEN
        val id = "$collection:$tokenId"

        val lazyErc721Dto = LazyErc721Dto(
            contract = Address.THREE(),
            tokenId = BigInteger.TEN,
            uri = "Test",
            creators = emptyList(),
            royalties = emptyList(),
            signatures = emptyList()
        )
        mockNftServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v0.1/items/${id.replace(":","%3A")}/lazy")
            )
            .respond(
                HttpResponse.response()
                    .withContentType(MediaType.JSON_UTF_8)
                    .withBody(mapper.writeValueAsBytes(lazyErc721Dto))
            )

        val result = nftItemApi.getNftLazyItemById(id).awaitFirst()
        assertThat(result).isEqualTo(lazyErc721Dto)
    }

    @Test
    fun `should go to nft service for mint`() = runBlocking<Unit> {
        val lazyErc721Dto = LazyErc721Dto(
            contract = Address.THREE(),
            tokenId = BigInteger.TEN,
            uri = "Test",
            creators = emptyList(),
            royalties = emptyList(),
            signatures = emptyList()
        )
        val itemDto = NftItemDto(
            id = "1",
            contract = Address.THREE(),
            tokenId = BigInteger.TEN,
            creators = emptyList(),
            supply = BigInteger.TEN,
            lazySupply = BigInteger.ZERO,
            owners = emptyList(),
            royalties = emptyList(),
            deleted = false,
            pending = emptyList(),
            date = null,
            meta = createItemMeta()
        )
        mockNftServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/v0.1/mints")
            )
            .respond(
                HttpResponse.response()
                    .withContentType(MediaType.JSON_UTF_8)
                    .withBody(mapper.writeValueAsBytes(itemDto))
            )

        val result = nftLazyMint.mintNftAsset(lazyErc721Dto).awaitFirst()
        assertThat(result).isEqualTo(itemDto)
    }

    @Test
    @Disabled
    fun `should go to nft-order service for item`() = runBlocking<Unit> {
        val itemDto = NftOrderItemDto(
            id = "1",
            contract = Address.THREE(),
            tokenId = BigInteger.TEN,
            creators = emptyList(),
            supply = BigInteger.TEN,
            lazySupply = BigInteger.ZERO,
            owners = emptyList(),
            royalties = emptyList(),
            bestSellOrder = null,
            bestBidOrder = null,
            totalStock = BigInteger.TEN,
            unlockable = true,
            pending = emptyList(),
            date = nowMillis(),
            meta = createItemMeta(),
            sellers = 0
        )
        mockNftServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v0.1/items/${itemDto.id}")
            )
            .respond(
                HttpResponse.response()
                    .withContentType(MediaType.JSON_UTF_8)
                    .withBody(mapper.writeValueAsBytes(itemDto))
            )

        val result = nftItemApi.getNftItemById(itemDto.id).awaitFirst()
        assertThat(result).isEqualTo(itemDto)
    }

    @Test
    @Disabled
    fun `should go to nft-order service for ownership`() = runBlocking<Unit> {
        val ownershipDto = NftOrderOwnershipDto(
            id = "1",
            contract = Address.THREE(),
            tokenId = BigInteger.TEN,
            owner = Address.THREE(),
            value = BigInteger.TEN,
            lazyValue = BigInteger.ZERO,
            date = nowMillis(),
            pending = emptyList(),
            bestSellOrder = null,
            creators = emptyList()
        )
        mockNftServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/v0.1/ownerships/${ownershipDto.id}")
            )
            .respond(
                HttpResponse.response()
                    .withContentType(MediaType.JSON_UTF_8)
                    .withBody(mapper.writeValueAsBytes(ownershipDto))
            )

        val result = nftOwnershipApi.getNftOwnershipById(ownershipDto.id).awaitFirst()
        assertThat(result).isEqualTo(ownershipDto)
    }
}
