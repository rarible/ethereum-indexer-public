package com.rarible.protocol.nft.api.controller

import com.rarible.protocol.dto.EthMetaStatusDto
import com.rarible.protocol.dto.EthereumApiErrorEntityNotFoundDto
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.api.test.AbstractIntegrationTest
import com.rarible.protocol.nft.api.test.End2EndTest
import com.rarible.protocol.nft.core.converters.dto.EthCollectionMetaDtoConverter
import com.rarible.protocol.nft.core.data.randomTokenProperties
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.MetaException
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import scalether.domain.AddressFactory

@End2EndTest
class CollectionControllerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Test
    fun `get by id - ok`() = runBlocking<Unit> {
        val token = createToken().copy(
            standard = TokenStandard.ERC721,
            features = setOf(TokenFeature.APPROVE_FOR_ALL, TokenFeature.BURN)
        )
        tokenRepository.save(token).awaitFirst()

        val collectionDto = nftCollectionApiClient.getNftCollectionById(token.id.hex()).awaitFirst()

        assertThat(collectionDto.id).isEqualTo(token.id)
        assertThat(collectionDto.type).isEqualTo(NftCollectionDto.Type.ERC721)
        assertThat(collectionDto.owner).isEqualTo(token.owner)
        assertThat(collectionDto.name).isEqualTo(token.name)
        assertThat(collectionDto.symbol).isEqualTo(token.symbol)
        assertThat(collectionDto.supportsLazyMint).isFalse()
        assertThat(collectionDto.features).containsExactlyInAnyOrder(
            NftCollectionDto.Features.APPROVE_FOR_ALL,
            NftCollectionDto.Features.BURN
        )
    }

    @Test
    fun `get by id - not found`() = runBlocking<Unit> {
        val token = createToken().copy(
            standard = TokenStandard.ERC721,
            features = setOf(TokenFeature.APPROVE_FOR_ALL, TokenFeature.BURN),
            status = ContractStatus.ERROR
        )
        tokenRepository.save(token).awaitFirst()

        val ex = assertThrows<NftCollectionControllerApi.ErrorGetNftCollectionById> {
            nftCollectionApiClient.getNftCollectionById(token.id.hex()).awaitFirst()
        }
        val error = ex.on404 as EthereumApiErrorEntityNotFoundDto

        assertThat(error.status).isEqualTo(HttpStatus.NOT_FOUND.value())
        assertThat(error.code).isEqualTo(EthereumApiErrorEntityNotFoundDto.Code.NOT_FOUND)
    }

    @Test
    fun `get by id - ok, with supportsLazyMint`() = runBlocking<Unit> {
        val token = createToken().copy(
            standard = TokenStandard.ERC721,
            features = setOf(TokenFeature.MINT_AND_TRANSFER)
        )
        tokenRepository.save(token).awaitFirst()

        val collectionDto = nftCollectionApiClient.getNftCollectionById(token.id.hex()).awaitFirst()

        assertThat(collectionDto.supportsLazyMint).isTrue()
        assertThat(collectionDto.features).containsExactlyInAnyOrder(
            NftCollectionDto.Features.MINT_AND_TRANSFER
        )
    }

    @Test
    fun `generate next tokenId - ok`() = runBlocking<Unit> {
        val token = createToken().copy(
            standard = TokenStandard.ERC721,
            features = setOf(TokenFeature.MINT_AND_TRANSFER)
        )
        tokenRepository.save(token).awaitFirst()

        val minter = AddressFactory.create()
        val tokenId = nftCollectionApiClient.generateNftTokenId(token.id.hex(), minter.hex()).awaitFirst()
        assertThat(tokenId).isNotNull
    }

    @Test
    fun `get collection meta - ok`() = runBlocking<Unit> {
        val token = createToken(standard = TokenStandard.ERC721)
        tokenRepository.save(token).awaitSingle()

        val tokenProperties = randomTokenProperties()
        val expected = TokenMeta(tokenProperties)

        coEvery { mockTokenStandardPropertiesResolver.resolve(eq(token.id)) } returns tokenProperties

        val result = nftCollectionApiClient.getCollectionMeta(token.id.toString()).awaitSingle()

        assertThat(result.status).isEqualTo(EthMetaStatusDto.OK)
        assertThat(result.meta).isEqualTo(EthCollectionMetaDtoConverter.convert(expected))
    }

    @Test
    fun `get collection meta - failed, meta exception`() = runBlocking<Unit> {
        val token = createToken(standard = TokenStandard.ERC721)
        tokenRepository.save(token).awaitSingle()

        coEvery { mockTokenStandardPropertiesResolver.resolve(eq(token.id)) } throws
            MetaException("json", MetaException.Status.UnparseableJson)

        val result = nftCollectionApiClient.getCollectionMeta(token.id.toString()).awaitSingle()

        assertThat(result.status).isEqualTo(EthMetaStatusDto.UNPARSEABLE_JSON)
        assertThat(result.meta).isNull()
    }

    @Test
    fun `get collection meta - failed, unexpected exception`() = runBlocking<Unit> {
        val token = createToken(standard = TokenStandard.ERC721)
        tokenRepository.save(token).awaitSingle()

        coEvery { mockTokenStandardPropertiesResolver.resolve(eq(token.id)) } throws RuntimeException("runtime")

        val result = nftCollectionApiClient.getCollectionMeta(token.id.toString()).awaitSingle()

        assertThat(result.status).isEqualTo(EthMetaStatusDto.ERROR)
        assertThat(result.meta).isNull()
    }
}
