package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class CollectionDtoConverterTest {

    companion object {
        @JvmStatic
        fun statusesSource(): Stream<Arguments> = Stream.of(
            Arguments.of(ContractStatus.PENDING, NftCollectionDto.Status.PENDING),
            Arguments.of(ContractStatus.ERROR, NftCollectionDto.Status.ERROR),
            Arguments.of(ContractStatus.CONFIRMED, NftCollectionDto.Status.CONFIRMED),
        )
    }

    @Test
    fun `convert - generic happy path`() {
        val token = buildToken()

        val actual = CollectionDtoConverter.convert(token)

        assertThat(actual.id).isEqualTo(token.id)
        assertThat(actual.type).isEqualTo(NftCollectionDto.Type.ERC721)
        assertThat(actual.status).isEqualTo(NftCollectionDto.Status.CONFIRMED)
        assertThat(actual.owner).isEqualTo(token.owner)
        assertThat(actual.name).isEqualTo(token.name)
        assertThat(actual.symbol).isEqualTo(token.symbol)
        assertThat(actual.features).containsExactly(NftCollectionDto.Features.APPROVE_FOR_ALL)
        assertThat(actual.supportsLazyMint).isFalse()
        assertThat(actual.minters).containsExactly(token.owner)
        assertThat(actual.scam).isEqualTo(token.scam)
    }

    @Test
    fun `convert - when lazy minting is supported`() {
        val token = buildToken().copy(features = setOf(TokenFeature.MINT_AND_TRANSFER))

        val actual = CollectionDtoConverter.convert(token)

        assertThat(actual.supportsLazyMint).isTrue()
    }

    @Test
    fun `convert - when minters is empty`() {
        val token = buildToken().copy(isRaribleContract = false)

        val actual = CollectionDtoConverter.convert(token)

        assertThat(actual.minters).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("statusesSource")
    fun `convert - statuses`(given: ContractStatus, expected: NftCollectionDto.Status) {
        val token = buildToken().copy(status = given)

        val actual = CollectionDtoConverter.convert(token)

        assertThat(actual.status).isEqualTo(expected)
    }

    private fun buildToken(): Token {
        return Token(
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
                scam = randomBoolean(),
            )
    }
}
