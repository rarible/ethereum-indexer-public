package com.rarible.protocol.nft.api.e2e.collection

import com.rarible.protocol.contracts.erc1155.rarible.ERC1155Rarible
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.MediaMeta
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.MediaMetaService
import com.rarible.protocol.nft.core.service.token.meta.TokenMetaService
import com.rarible.protocol.nft.core.service.token.meta.descriptors.OpenseaDescriptor
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardDescriptor
import io.daonomic.rpc.domain.Word
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils.setField
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.TransactionReceipt
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.net.URI


@End2EndTest
class CollectionMetaFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var tokenMetaService: TokenMetaService

    @SpyBean
    private lateinit var standardDescriptor: StandardDescriptor

    @SpyBean
    private lateinit var openseaDescriptor: OpenseaDescriptor

    val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

    private lateinit var userSender: MonoSigningTransactionSender
    private lateinit var erc721: ERC721Rarible

    @BeforeEach
    fun before() = runBlocking<Unit> {
        userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }
        erc721 = ERC721Rarible.deployAndWait(userSender, poller).awaitFirst()
        erc721.__ERC721Rarible_init(
            "Feudalz",
            "FEUDALZ",
            "baseURI",
            "ipfs://QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"
        ).execute().verifySuccess()
    }

    @Test
    fun `should get meta and parse from ipfs`() = runBlocking<Unit> {
        val tokenToSave = createToken().copy(
            id = erc721.address(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(tokenToSave).awaitSingle()

        setField(standardDescriptor, "client", mockSuccessIpfsResponse())
        setField(tokenMetaService, "mediaMetaService", mockMediaIpfsResponse())

        val metaDto = nftCollectionApiClient.getNftCollectionById(tokenToSave.id.toString()).awaitSingle().meta!!

        verify(standardDescriptor, times(1)).resolve(any())
        verify(openseaDescriptor, times(0)).resolve(any())

        assertThat(metaDto.name).isEqualTo("Feudalz")
        assertThat(metaDto.description).isEqualTo("Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.")
        assertThat(metaDto.image).isEqualTo(
            NftMediaDto(
                url = mapOf("ORIGINAL" to "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d"),
                meta = mapOf("ORIGINAL" to NftMediaMetaDto(type = "image/png", width = 256, height = 256))
            )
        )
        assertThat(metaDto.external_link).isEqualTo("https://feudalz.io")
        assertThat(metaDto.seller_fee_basis_points).isEqualTo(250)
        assertThat(metaDto.fee_recipient).isEqualTo(Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"))
    }

    @Test
    fun `should get meta and parse from ipfs -- ERC1155`() = runBlocking<Unit> {
        val erc1155 = ERC1155Rarible.deployAndWait(userSender, poller).awaitFirst()
        erc1155.__ERC1155Rarible_init(
            "Feudalz",
            "FEUDALZ",
            "baseURI",
            "ipfs://QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"
        ).execute().verifySuccess()
        val tokenToSave = createToken().copy(
            id = erc1155.address(),
            standard = TokenStandard.ERC1155
        )
        tokenRepository.save(tokenToSave).awaitSingle()

        setField(standardDescriptor, "client", mockSuccessIpfsResponse())
        setField(tokenMetaService, "mediaMetaService", mockMediaIpfsResponse())

        val metaDto = nftCollectionApiClient.getNftCollectionById(tokenToSave.id.toString()).awaitSingle().meta!!

        verify(standardDescriptor, times(1)).resolve(any())
        verify(openseaDescriptor, times(0)).resolve(any())

        assertThat(metaDto.name).isEqualTo("Feudalz")
        assertThat(metaDto.description).isEqualTo("Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.")
        assertThat(metaDto.image).isEqualTo(
            NftMediaDto(
                url = mapOf("ORIGINAL" to "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d"),
                meta = mapOf("ORIGINAL" to NftMediaMetaDto(type = "image/png", width = 256, height = 256))
            )
        )
        assertThat(metaDto.external_link).isEqualTo("https://feudalz.io")
        assertThat(metaDto.seller_fee_basis_points).isEqualTo(250)
        assertThat(metaDto.fee_recipient).isEqualTo(Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"))
    }

    @Test
    fun `should get meta and parse from opensea`() = runBlocking<Unit> {
        val tokenToSave = createToken().copy(
            id = erc721.address(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(tokenToSave).awaitSingle()

        setField(standardDescriptor, "client", mockNotFoundIpfsResponse())
        setField(openseaDescriptor, "client", mockOpenSeaResponse())
        setField(tokenMetaService, "mediaMetaService", mockMediaOpenseaResponse())

        val metaDto = nftCollectionApiClient.getNftCollectionById(tokenToSave.id.toString()).awaitSingle().meta!!

        verify(standardDescriptor, times(1)).resolve(any())
        verify(openseaDescriptor, times(1)).resolve(any())

        assertThat(metaDto.name).isEqualTo("Feudalz")
        assertThat(metaDto.description).isEqualTo("Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.")
        assertThat(metaDto.image).isEqualTo(
            NftMediaDto(
                url = mapOf("ORIGINAL" to "https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120"),
                meta = mapOf("ORIGINAL" to NftMediaMetaDto(type = "image/png", width = 256, height = 256))
            )
        )
        assertThat(metaDto.external_link).isEqualTo("https://feudalz.io")
        assertThat(metaDto.seller_fee_basis_points).isEqualTo(250)
        assertThat(metaDto.fee_recipient).isEqualTo(Address.apply("0xc00f4b8022e4dc7f086d703328247cb6adf26858"))
    }

    @Test
    fun `should get empty meta`() = runBlocking<Unit> {
        val tokenToSave = createToken().copy(
            id = erc721.address(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(tokenToSave).awaitSingle()

        setField(standardDescriptor, "client", mockNotFoundIpfsResponse())

        val metaDto = nftCollectionApiClient.getNftCollectionById(tokenToSave.id.toString()).awaitSingle().meta!!

        verify(standardDescriptor, times(1)).resolve(any())
        verify(openseaDescriptor, times(1)).resolve(any())

        assertThat(metaDto.name).isEqualTo("Untitled")
        assertThat(metaDto.description).isEqualTo(null)
    }

    @Test
    fun `should get empty meta on parsing error`() = runBlocking<Unit> {
        val tokenToSave = createToken().copy(
            id = erc721.address(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(tokenToSave).awaitSingle()

        setField(standardDescriptor, "client", mockBrokenJsonIpfsResponse())

        val metaDto = nftCollectionApiClient.getNftCollectionById(tokenToSave.id.toString()).awaitSingle().meta!!

        verify(standardDescriptor, times(1)).resolve(any())
        verify(openseaDescriptor, times(1)).resolve(any())

        assertThat(metaDto.name).isEqualTo("Untitled")
        assertThat(metaDto.description).isEqualTo(null)
    }

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }

    private fun mockSuccessIpfsResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                assertThat(request.url()).isEqualTo(URI("https://rarible.mypinata.cloud/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"))
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "application/json")
                        .body("ipfs.json".asResource())
                        .build()
                )
            }.build()
    }

    private fun mockOpenSeaResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                assertThat(request.url().toStr()).startsWith("https://api.opensea.io/api/v1/asset_contract/0x")
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "application/json")
                        .body("opensea.json".asResource())
                        .build()
                )
            }.build()
    }

    private fun mockNotFoundIpfsResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                assertThat(request.url()).isEqualTo(URI("https://rarible.mypinata.cloud/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"))
                Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
            }.build()
    }

    private fun mockBrokenJsonIpfsResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "plain/text")
                        .body("Hello world!")
                        .build()
                )
            }.build()
    }

    private fun mockMediaIpfsResponse(): MediaMetaService {
        val mock: MediaMetaService = mockk()
        coEvery { mock.getMediaMeta(eq("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d")) } returns
            MediaMeta(
                type = "image/png", width = 256, height = 256
            )
        return mock
    }

    private fun mockMediaOpenseaResponse(): MediaMetaService {
        val mock: MediaMetaService = mockk()
        coEvery { mock.getMediaMeta(eq("https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120")) } returns
            MediaMeta(
                type = "image/png", width = 256, height = 256
            )
        return mock
    }

    fun String.asResource() = this.javaClass::class.java.getResource("/data/token/response/$this").readText()

}
