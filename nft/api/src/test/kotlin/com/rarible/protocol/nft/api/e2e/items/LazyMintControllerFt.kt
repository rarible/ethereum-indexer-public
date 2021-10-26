package com.rarible.protocol.nft.api.e2e.items

import com.ninjasquad.springmockk.MockkBean
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createLazyErc1155Dto
import com.rarible.protocol.nft.api.e2e.data.createLazyErc721Dto
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger
import java.util.stream.Stream

@End2EndTest
class LazyMintControllerFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @MockkBean
    private lateinit var lazyNftValidator: LazyNftValidator

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    companion object {
        @JvmStatic
        fun lazyNft(): Stream<LazyNftDto> {

            return Stream.of(
                createLazyErc721Dto()
                    .run {
                        val creator = Address.apply("0xd376a12278f253a0a6333ce16cf81ac48a791770")
                        val tokenId = BigInteger("95647611115250239671767719211153254200250262171035878288383384760863349014635")
                        copy(tokenId = tokenId, creators = listOf(PartDto(creator, 10000)))
                    },
                createLazyErc1155Dto()
                    .run {
                        val creator = "0376a12278f253a0a6333ce16cf81ac48a791770"
                        val idPart = "000000000000000000000001"

                        val validTokenId = creator + idPart
                        copy(tokenId = BigInteger(validTokenId, 16), creators = listOf(PartDto(Address.apply(creator), 10000)))
                    }
            )
        }
    }

    @ParameterizedTest
    @MethodSource("lazyNft")
    fun `should mint lazy item`(lazyItemDto: LazyNftDto) = runBlocking {
        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()

        val itemId = ItemId(lazyItemDto.contract, EthUInt256(lazyItemDto.tokenId))

        coEvery { lazyNftValidator.validate(any()) } returns ValidationResult.Valid

        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()
        assertThat(itemDto.id).isEqualTo(itemId.decimalStringValue)
        assertThat(itemDto.contract).isEqualTo(lazyItemDto.contract)
        assertThat(itemDto.tokenId).isEqualTo(lazyItemDto.tokenId)

        when (lazyItemDto) {
            is LazyErc1155Dto -> {
                assertThat(itemDto.supply).isEqualTo(lazyItemDto.supply)
                assertThat(itemDto.lazySupply).isEqualTo(lazyItemDto.supply)
            }
            is LazyErc721Dto -> {
                assertThat(itemDto.supply).isEqualTo(EthUInt256.ONE.value)
                assertThat(itemDto.lazySupply).isEqualTo(EthUInt256.ONE.value)
            }
        }

        assertThat(itemDto.owners).isEqualTo(listOf(lazyItemDto.creators.first().account))

        assertThat(itemDto.royalties.size).isEqualTo(lazyItemDto.royalties.size)

        itemDto.royalties.forEachIndexed { index, royaltyDto ->
            assertThat(royaltyDto.account).isEqualTo(lazyItemDto.royalties[index].account)
            assertThat(royaltyDto.value).isEqualTo(lazyItemDto.royalties[index].value)
        }

        assertThat(itemDto.creators).hasSize(lazyItemDto.creators.size)

        itemDto.creators.forEachIndexed { index, creatorDto ->
            assertThat(creatorDto.account).isEqualTo(lazyItemDto.creators[index].account)
            assertThat(creatorDto.value).isEqualTo(lazyItemDto.creators[index].value)
        }

        val lazyMint = lazyNftItemHistoryRepository.findLazyMintById(itemId).awaitFirst()
        assertThat(lazyMint.token).isEqualTo(lazyItemDto.contract)
        assertThat(lazyMint.tokenId.value).isEqualTo(lazyItemDto.tokenId)

        lazyMint.creators.forEachIndexed { index, it ->
            assertThat(it)
                .hasFieldOrPropertyWithValue(Part::account.name, lazyItemDto.creators[index].account)
                .hasFieldOrPropertyWithValue(Part::value.name, lazyItemDto.creators[index].value)
        }

        lazyMint.royalties.forEachIndexed { index, royalty ->
            assertThat(royalty)
                .hasFieldOrPropertyWithValue(Part::account.name, lazyItemDto.royalties[index].account)
                .hasFieldOrPropertyWithValue(Part::value.name, lazyItemDto.royalties[index].value)
        }
    }

    @Test
    fun `should get bad request if token id not start with first creator address`() = runBlocking<Unit> {
        val lazyNftDto = createLazyErc721Dto()
        val token = createToken().copy(id = lazyNftDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))

        tokenRepository.save(token).awaitFirst()

        assertThatThrownBy {
            runBlocking { nftLazyMintApiClient.mintNftAsset(lazyNftDto).awaitFirst() }
        }
    }
}
