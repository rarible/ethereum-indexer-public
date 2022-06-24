package com.rarible.protocol.nft.api.e2e.collection

import com.rarible.core.cache.Cache
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.MetaContentDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
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
import java.util.Date

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
            externalUri = "https://feudalz.io",
            feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
            sellerFeeBasisPoints = 250,
            content = ContentBuilder.getTokenMetaContent(
                imageOriginal = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d"
            )
        )
        coEvery { mockMediaMetaService.getMediaMetaFromCache("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d", any()) } returns ContentMeta(
            type = "image/png",
            width = 256,
            height = 256
        )

        val metaDto = nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        coVerify(exactly = 1) { mockTokenStandardPropertiesResolver.resolve(token.id) }
        coVerify(exactly = 0) { mockTokenOpenseaPropertiesResolver.resolve(token.id) }

        assertThat(metaDto.name).isEqualTo("Feudalz")
        assertThat(metaDto.description).isEqualTo("Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.")
        assertThat(metaDto.content[0]).isEqualTo(
            ImageContentDto(
                url = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
                representation = MetaContentDto.Representation.ORIGINAL,
                fileName = null,
                mimeType = "image/png",
                height = 256,
                size = null,
                width = 256
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
            externalUri = "https://feudalz.io",
            feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
            sellerFeeBasisPoints = 250,
            content = ContentBuilder.getTokenMetaContent(
                imageOriginal = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d"
            )
        )
        coEvery { mockMediaMetaService.getMediaMetaFromCache("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d", any()) } returns ContentMeta(
            type = "image/png",
            width = 256,
            height = 256
        )

        nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        val cached = mongo.findById(token.id.prefixed(), Cache::class.java, TOKEN_METADATA_COLLECTION).awaitSingle()
        assertThat(cached.data).isNotNull
    }

    @Test
    fun `should get from cache`() = runBlocking<Unit> {
        mongo.save(
            Cache(
                id = token.id.prefixed(),
                data = TokenPropertiesService.CachedTokenProperties(
                    properties = TokenProperties(
                        name = "Feudalz",
                        description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
                        externalUri = "https://feudalz.io",
                        feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
                        sellerFeeBasisPoints = 250,
                        content = ContentBuilder.getTokenMetaContent(
                            imageOriginal = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d"
                        )
                    ),
                    fetchAt = Instant.now()
                ),
                updateDate = Date.from(Instant.now())
            ), TOKEN_METADATA_COLLECTION
        ).awaitSingle()
        coEvery { mockMediaMetaService.getMediaMetaFromCache("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d", any()) } returns ContentMeta(
            type = "image/png",
            width = 256,
            height = 256
        )

        val metaDto = nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        coVerify(exactly = 0) { mockTokenStandardPropertiesResolver.resolve(token.id) }
        coVerify(exactly = 0) { mockTokenOpenseaPropertiesResolver.resolve(token.id) }

        assertThat(metaDto.name).isEqualTo("Feudalz")
        assertThat(metaDto.description).isEqualTo("Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.")
        assertThat(metaDto.content[0]).isEqualTo(
            ImageContentDto(
                url = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
                representation = MetaContentDto.Representation.ORIGINAL,
                fileName = null,
                mimeType = "image/png",
                height = 256,
                width = 256,
                size = null
            )
        )
        assertThat(metaDto.external_link).isEqualTo("https://feudalz.io")
        assertThat(metaDto.seller_fee_basis_points).isEqualTo(250)
        assertThat(metaDto.fee_recipient).isEqualTo(Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"))
    }

    @Test
    fun `should get reset cache`() = runBlocking<Unit> {
        mongo.save(
            Cache(
                id = token.id.prefixed(),
                data = TokenPropertiesService.CachedTokenProperties(
                    properties = TokenProperties(
                        name = "Feudalz",
                        description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
                        externalUri = "https://feudalz.io",
                        feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
                        sellerFeeBasisPoints = 250,
                        content = ContentBuilder.getTokenMetaContent(
                            imageOriginal = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d"
                        )
                    ),
                    fetchAt = Instant.now()
                ),
                updateDate = Date.from(Instant.now())
            ), TOKEN_METADATA_COLLECTION
        ).awaitSingle()
        coEvery { mockMediaMetaService.getMediaMetaFromCache("https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d", any()) } returns ContentMeta(
            type = "image/png",
            width = 256,
            height = 256
        )

        nftCollectionApiClient.resetNftCollectionMetaById(token.id.toString()).awaitSingleOrNull()
        val cached =
            mongo.findById(token.id.prefixed(), Cache::class.java, TOKEN_METADATA_COLLECTION).awaitFirstOrNull()
        assertThat(cached).isNull()
    }

    @Test
    fun `should get meta from opensea`() = runBlocking<Unit> {
        coEvery { mockTokenStandardPropertiesResolver.resolve(eq(token.id)) } returns null
        coEvery { mockTokenOpenseaPropertiesResolver.resolve(eq(token.id)) } returns TokenProperties(
            name = "Feudalz",
            description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
            externalUri = "https://feudalz.io",
            feeRecipient = Address.apply("0xc00f4b8022e4dc7f086d703328247cb6adf26858"),
            sellerFeeBasisPoints = 250,
            content = ContentBuilder.getTokenMetaContent(
                imageOriginal = "https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120"
            )
        )
        coEvery { mockMediaMetaService.getMediaMetaFromCache("https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120", any()) } returns ContentMeta(
            type = "image/png",
            width = 256,
            height = 256
        )

        val metaDto = nftCollectionApiClient.getNftCollectionById(token.id.toString()).awaitSingle().meta!!

        coVerify(exactly = 1) { mockTokenStandardPropertiesResolver.resolve(token.id) }
        coVerify(exactly = 1) { mockTokenOpenseaPropertiesResolver.resolve(token.id) }

        assertThat(metaDto.name).isEqualTo("Feudalz")
        assertThat(metaDto.description).isEqualTo("Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.")
        assertThat(metaDto.content[0]).isEqualTo(
            ImageContentDto(
                url = "https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120",
                representation = MetaContentDto.Representation.ORIGINAL,
                fileName = null,
                mimeType = "image/png",
                height = 256,
                width = 256,
                size = null
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
