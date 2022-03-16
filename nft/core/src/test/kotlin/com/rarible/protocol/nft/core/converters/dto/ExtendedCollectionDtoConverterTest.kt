package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.ExtendedToken
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class ExtendedCollectionDtoConverterTest {

    @InjectMockKs
    lateinit var converter: ExtendedCollectionDtoConverter

    @MockK
    private lateinit var collectionType: NftCollectionDto.Type

    @MockK
    private lateinit var collectionFeature: NftCollectionDto.Features

    @MockK
    private lateinit var collectionMeta: NftCollectionMetaDto

    companion object {
        @JvmStatic
        fun statusesSource(): Stream<Arguments> = Stream.of(
            Arguments.of(ContractStatus.PENDING, NftCollectionDto.Status.PENDING),
            Arguments.of(ContractStatus.ERROR, NftCollectionDto.Status.ERROR),
            Arguments.of(ContractStatus.CONFIRMED, NftCollectionDto.Status.CONFIRMED),
        )
    }

    @BeforeEach
    fun setUp() {
        mockkObject(CollectionTypeDtoConverter, CollectionFeatureDtoConverter, NftCollectionMetaDtoConverter)
        every { CollectionTypeDtoConverter.convert(any()) } returns collectionType
        every { CollectionFeatureDtoConverter.convert(any()) } returns collectionFeature
        every { NftCollectionMetaDtoConverter.convert(any()) } returns collectionMeta
    }

    @AfterEach
    fun tearDown() {
        clearMocks(CollectionTypeDtoConverter, CollectionFeatureDtoConverter, NftCollectionMetaDtoConverter)
    }

    @Test
    fun `convert - generic happy path`() {
        val token = buildToken()

        val actual = converter.convert(token)

        assertThat(actual.id).isEqualTo(token.token.id)
        assertThat(actual.type).isEqualTo(collectionType)
        assertThat(actual.status).isEqualTo(NftCollectionDto.Status.CONFIRMED)
        assertThat(actual.owner).isEqualTo(token.token.owner)
        assertThat(actual.name).isEqualTo(token.token.name)
        assertThat(actual.symbol).isEqualTo(token.token.symbol)
        assertThat(actual.features).containsExactly(collectionFeature)
        assertThat(actual.supportsLazyMint).isFalse()
        assertThat(actual.minters).containsExactly(token.token.owner)
        assertThat(actual.meta).isEqualTo(collectionMeta)

        verify {
            CollectionTypeDtoConverter.convert(token.token.standard)
            CollectionFeatureDtoConverter.convert(token.token.features.first())
            NftCollectionMetaDtoConverter.convert(token.tokenMeta)
        }
        confirmVerified(CollectionTypeDtoConverter, CollectionFeatureDtoConverter, NftCollectionMetaDtoConverter)
    }

    @Test
    fun `convert - when lazy minting is supported`() {
        val token = ExtendedToken(buildToken().token.copy(features = setOf(TokenFeature.MINT_AND_TRANSFER)), mockk())

        val actual = converter.convert(token)

        assertThat(actual.supportsLazyMint).isTrue()
    }

    @Test
    fun `convert - when minters is empty`() {
        val token = ExtendedToken(buildToken().token.copy(isRaribleContract = false), mockk())

        val actual = converter.convert(token)

        assertThat(actual.minters).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("statusesSource")
    fun `convert - statuses`(given: ContractStatus, expected: NftCollectionDto.Status) {
        val token = ExtendedToken(buildToken().token.copy(status = given), mockk())

        val actual = converter.convert(token)

        assertThat(actual.status).isEqualTo(expected)
    }

    private fun buildToken(): ExtendedToken {
        return ExtendedToken(
            token = Token(
                id = randomAddress(),
                owner = randomAddress(),
                name = randomString(),
                symbol = randomString(),
                status = ContractStatus.CONFIRMED,
                features = setOf(TokenFeature.APPROVE_FOR_ALL),
                lastEventId = randomString(),
                standard = TokenStandard.ERC721,
                version = randomLong(),
                isRaribleContract = true,
                deleted = false,
                revertableEvents = emptyList(),
            ),
            mockk(),
        )
    }
}
