package com.rarible.protocol.order.api.service.order.validation

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.nft.model.LazyERC721
import com.rarible.ethereum.nft.model.Part
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.stream.Stream

internal class LazyAssetValidatorTest {

    private val delegate = mockk<LazyNftValidator>()
    private val nftCollectionApi = mockk<NftCollectionControllerApi>()

    private val lazyAssetValidator = LazyAssetValidator(delegate, nftCollectionApi)
    
    @BeforeEach
    fun setup() {
        clearMocks(delegate, nftCollectionApi)
    }

    @Test
    fun `should validate lazy asset`() = runBlocking {
        val creator = Address.apply("0x25646b08d9796ceda5fb8ce0105a51820740c049")
        val token = AddressFactory.create()
        val tokenId = EthUInt256.of("0x25646b08d9796ceda5fb8ce0105a51820740c04900000000000000000000000a")

        val lazyNft = LazyERC721(
            token = token,
            tokenId = tokenId.value,
            uri = randomString(),
            creators = listOf(Part(creator, 10000)),
            signatures = listOf(),
            royalties = listOf()
        )

        val collection = mockk<NftCollectionDto> {
            every { features } returns listOf(NftCollectionDto.Features.MINT_AND_TRANSFER)
        }

        every { nftCollectionApi.getNftCollectionById(token.hex()) } returns Mono.just(collection)
        coEvery { delegate.validate(lazyNft) } returns ValidationResult.Valid

        lazyAssetValidator.validate(lazyNft, "take")

        coVerify(exactly = 1) { delegate.validate(lazyNft) }
    }

    companion object {
        @JvmStatic
        fun invalidResults(): Stream<ValidationResult> {
            return Stream.of(
                ValidationResult.InvalidCreatorAndSignatureSize,
                ValidationResult.NotUniqCreators,
                ValidationResult.InvalidCreatorSignature(emptyList())
            )
        }
    }

    @ParameterizedTest
    @MethodSource("invalidResults")
    fun `should throw exception on invalid result of delegate`(invalidResult: ValidationResult) = runBlocking {
        val token = randomAddress()

        val lazyNft = LazyERC721(
            token = token,
            tokenId = randomBigInt(),
            uri = randomString(),
            creators = listOf(Part(randomAddress(), 10000)),
            signatures = listOf(),
            royalties = listOf()
        )

        val collection = mockk<NftCollectionDto> {
            every { features } returns emptyList()
        }

        every { nftCollectionApi.getNftCollectionById(token.hex()) } returns Mono.just(collection)
        coEvery { delegate.validate(lazyNft) } returns invalidResult

        assertThrows<OrderUpdateException> {
            runBlocking {
                lazyAssetValidator.validate(lazyNft, "take")
            }
        }

        coVerify(exactly = 1) { delegate.validate(lazyNft) }
    }

    @Test
    fun `should throw exception on invalid tokenId`() = runBlocking {
        val token = AddressFactory.create()
        val tokenId = BigInteger.ONE

        val lazyNft = LazyERC721(
            token = token,
            tokenId = tokenId,
            uri = randomString(),
            creators = listOf(Part(randomAddress(), 10000)),
            signatures = listOf(),
            royalties = listOf()
        )

        val collection = mockk<NftCollectionDto> {
            every { features } returns listOf(NftCollectionDto.Features.MINT_AND_TRANSFER)
        }

        every { nftCollectionApi.getNftCollectionById(token.hex()) } returns Mono.just(collection)

        assertThrows<OrderUpdateException> {
            runBlocking {
                lazyAssetValidator.validate(lazyNft, "take")
            }
        }

        coVerify(exactly = 0) { delegate.validate(lazyNft) }
    }
}
