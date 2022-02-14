package com.rarible.protocol.nft.api.e2e.collection

import com.rarible.protocol.dto.EthereumApiErrorEntityNotFoundDto
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import scalether.domain.AddressFactory

@End2EndTest
class GetCollectionFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Test
    fun `should get item by id`() = runBlocking<Unit> {
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
    fun `shouldn't get error collection by id`() = runBlocking<Unit> {
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

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), error.status)
        Assertions.assertEquals(EthereumApiErrorEntityNotFoundDto.Code.NOT_FOUND, error.code)
    }

    @Test
    fun `should get item by id with set supportsLazyMint flag`() = runBlocking<Unit> {
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
    fun `should generate text token id`() = runBlocking<Unit> {
        val token = createToken().copy(
            standard = TokenStandard.ERC721,
            features = setOf(TokenFeature.MINT_AND_TRANSFER)
        )
        tokenRepository.save(token).awaitFirst()

        val minter = AddressFactory.create()
        val tokenId = nftCollectionApiClient.generateNftTokenId(token.id.hex(), minter.hex()).awaitFirst()
        assertThat(tokenId).isNotNull
    }
}
