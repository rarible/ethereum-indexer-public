package com.rarible.protocol.nft.api.service.mint

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.ethereum.sign.service.InvalidSignatureException
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import io.daonomic.rpc.domain.Binary
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.abi.Uint256Type
import java.math.BigInteger

class LazyNftValidatorTest {

    private val delegate: com.rarible.ethereum.nft.validation.LazyNftValidator = mockk()
    private val tokenRepository: TokenRepository = mockk()

    private val validator: LazyNftValidator = LazyNftValidator(delegate, tokenRepository)

    @BeforeEach
    fun beforeEach() {
        clearMocks(delegate, tokenRepository)
    }

    @Test
    fun `token not found`() {
        val lazyNftDto = randomLazyErc721Dto()

        coEvery { tokenRepository.findById(lazyNftDto.contract) } returns Mono.empty()

        assertThrows(EntityNotFoundApiException::class.java) {
            runBlocking { validator.validate(lazyNftDto) }
        }
    }

    @Test
    fun `token hasn't required feature`() {
        val lazyNftDto = randomLazyErc721Dto()

        coEvery { tokenRepository.findById(lazyNftDto.contract) } returns Mono.just(
            randomToken(lazyNftDto).copy(features = emptySet())
        )

        assertThrows(ValidationApiException::class.java) {
            runBlocking { validator.validate(lazyNftDto) }
        }
    }

    @Test
    @Disabled // TODO how to check it with tokenId < 32 bytes?
    fun `token size lesser than creator size`() {
        val lazyNftDto = randomLazyErc721Dto()
            .copy(tokenId = BigInteger.ONE)

        coEvery { tokenRepository.findById(lazyNftDto.contract) } returns Mono.just(randomToken(lazyNftDto))

        assertThrows(ValidationApiException::class.java) {
            runBlocking { validator.validate(lazyNftDto) }
        }
    }

    @Test
    fun `creator is not a subarray of token`() {
        val lazyNftDto = randomLazyErc721Dto()
            .copy(tokenId = BigInteger.ONE)

        coEvery { tokenRepository.findById(lazyNftDto.contract) } returns Mono.just(randomToken(lazyNftDto))

        assertThrows(ValidationApiException::class.java) {
            runBlocking { validator.validate(lazyNftDto) }
        }
    }

    @Test
    fun `incorrect signature`() {
        val lazyNftDto = randomLazyErc721Dto()

        coEvery { tokenRepository.findById(lazyNftDto.contract) } returns Mono.just(randomToken(lazyNftDto))
        coEvery { delegate.validate(any()) } throws InvalidSignatureException("abc")

        val e = assertThrows(ValidationApiException::class.java) {
            runBlocking { validator.validate(lazyNftDto) }
        }
        assertThat(e.message).isEqualTo("abc")
    }

    @Test
    fun `validation passed`() {
        val lazyNftDto = randomLazyErc721Dto()

        coEvery { tokenRepository.findById(lazyNftDto.contract) } returns Mono.just(randomToken(lazyNftDto))
        coEvery { delegate.validate(any()) } returns ValidationResult.Valid

        runBlocking { validator.validate(lazyNftDto) }
    }

    @Test
    fun `validation failed`() {
        val lazyNftDto = randomLazyErc721Dto()

        coEvery { tokenRepository.findById(lazyNftDto.contract) } returns Mono.just(randomToken(lazyNftDto))
        coEvery { delegate.validate(any()) } returns ValidationResult.NotUniqCreators

        assertThrows(ValidationApiException::class.java) {
            runBlocking { validator.validate(lazyNftDto) }
        }
    }

    private fun randomLazyErc721Dto(): LazyErc721Dto {
        val creator = randomAddress()
        val bs = Binary.apply(creator.bytes().plus(ByteArray(12)))
        val tokenId = Uint256Type.decode(bs, 0).value()

        return LazyErc721Dto(
            contract = randomAddress(),
            tokenId = tokenId,
            uri = randomString(),
            creators = listOf(PartDto(creator, 10000)),
            royalties = emptyList(),
            signatures = listOf(randomBinary())
        )
    }

    private fun randomToken(dto: LazyErc721Dto): Token {
        return Token(
            id = dto.contract,
            name = randomString(),
            status = ContractStatus.CONFIRMED,
            features = setOf(TokenFeature.MINT_AND_TRANSFER),
            standard = TokenStandard.ERC721
        )
    }

}