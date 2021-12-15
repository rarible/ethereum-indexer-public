package com.rarible.protocol.nft.api.e2e.collection

import com.rarible.core.cache.Cache
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.MediaMeta
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.Companion.TOKEN_METADATA_COLLECTION
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Instant
import java.util.*


@End2EndTest
class CollectionMetaFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    private lateinit var token: Token

    @BeforeEach
    fun before() = runBlocking<Unit> {
        token = createToken().copy(
            id = AddressFactory.create(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(token).awaitSingle()
    }

    @Test
    fun `should get meta from contract`() = runBlocking<Unit> {
        coEvery { mockTokenStandardPropertiesResolver.resolve(eq(token.id)) } returns TokenProperties(
            name = "Feudalz",
            description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
            image= "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
            externalLink = "https://feudalz.io",
            feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
            sellerFeeBasisPoints = 250
        )
        coEvery { mockMediaMetaService.getMediaMeta("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d") } returns MediaMeta(type = "image/png", width = 256, height = 256)

        val metaDto = nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        coVerify(exactly = 1) { mockTokenStandardPropertiesResolver.resolve(token.id) }
        coVerify(exactly = 0) { mockTokenOpenseaPropertiesResolver.resolve(token.id) }

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
    fun `should save to cache`() = runBlocking<Unit> {
        coEvery { mockTokenStandardPropertiesResolver.resolve(eq(token.id)) } returns TokenProperties(
            name = "Feudalz",
            description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
            image= "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
            externalLink = "https://feudalz.io",
            feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
            sellerFeeBasisPoints = 250
        )
        coEvery { mockMediaMetaService.getMediaMeta("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d") } returns MediaMeta(type = "image/png", width = 256, height = 256)

        nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        val cached = mongo.findById(token.id.prefixed(), Cache::class.java, TOKEN_METADATA_COLLECTION).awaitSingle()
        assertThat(cached.data).isNotNull
    }

    @Test
    fun `should get from cache`() = runBlocking<Unit> {
        mongo.save(Cache(
            id = token.id.prefixed(),
            data = TokenPropertiesService.CachedTokenProperties(
                properties = TokenProperties(
                    name = "Feudalz",
                    description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
                    externalLink = "https://feudalz.io",
                    image = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
                    feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
                    sellerFeeBasisPoints = 250
                ),
                fetchAt = Instant.now()
            ),
            updateDate = Date.from(Instant.now())
        ), TOKEN_METADATA_COLLECTION).awaitSingle()
        coEvery { mockMediaMetaService.getMediaMeta("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d") } returns MediaMeta(type = "image/png", width = 256, height = 256)

        val metaDto = nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        coVerify(exactly = 0) { mockTokenStandardPropertiesResolver.resolve(token.id) }
        coVerify(exactly = 0) { mockTokenOpenseaPropertiesResolver.resolve(token.id) }

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
    fun `should get reset cache`() = runBlocking<Unit> {
        mongo.save(Cache(
            id = token.id.prefixed(),
            data = TokenPropertiesService.CachedTokenProperties(
                properties = TokenProperties(
                    name = "Feudalz",
                    description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
                    externalLink = "https://feudalz.io",
                    image = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
                    feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
                    sellerFeeBasisPoints = 250
                ),
                fetchAt = Instant.now()
            ),
            updateDate = Date.from(Instant.now())
        ), TOKEN_METADATA_COLLECTION).awaitSingle()
        coEvery { mockMediaMetaService.getMediaMeta("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d") } returns MediaMeta(type = "image/png", width = 256, height = 256)

        nftCollectionApiClient.resetNftCollectionMetaById(token.id.toString()).awaitSingleOrNull()
        val cached = mongo.findById(token.id.prefixed(), Cache::class.java, TOKEN_METADATA_COLLECTION).awaitFirstOrNull()
        assertThat(cached).isNull()
    }

    @Test
    fun `should get meta from opensea`() = runBlocking<Unit> {
        coEvery { mockTokenStandardPropertiesResolver.resolve(eq(token.id)) } returns null
        coEvery { mockTokenOpenseaPropertiesResolver.resolve(eq(token.id)) } returns TokenProperties(
            name = "Feudalz",
            description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
            image= "https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120",
            externalLink = "https://feudalz.io",
            feeRecipient = Address.apply("0xc00f4b8022e4dc7f086d703328247cb6adf26858"),
            sellerFeeBasisPoints = 250
        )
        coEvery { mockMediaMetaService.getMediaMeta("https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120") } returns MediaMeta(type = "image/png", width = 256, height = 256)

        val metaDto = nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        coVerify(exactly = 1) { mockTokenStandardPropertiesResolver.resolve(token.id) }
        coVerify(exactly = 1) { mockTokenOpenseaPropertiesResolver.resolve(token.id) }

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
        coEvery { mockTokenStandardPropertiesResolver.resolve(eq(token.id)) } returns null
        coEvery { mockTokenOpenseaPropertiesResolver.resolve(eq(token.id)) } returns null

        val metaDto = nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        coVerify(exactly = 1) { mockTokenStandardPropertiesResolver.resolve(token.id) }
        coVerify(exactly = 1) { mockTokenOpenseaPropertiesResolver.resolve(token.id) }

        assertThat(metaDto.name).isEqualTo("Untitled")
        assertThat(metaDto.description).isEqualTo(null)
    }

}
