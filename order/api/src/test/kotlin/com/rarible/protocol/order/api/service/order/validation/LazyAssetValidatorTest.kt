package com.rarible.protocol.order.api.service.order.validation

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.nft.model.LazyNft
import com.rarible.ethereum.nft.model.Part
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.convert.ConversionService
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.stream.Stream

internal class LazyAssetValidatorTest {

    private val delegate = mockk<LazyNftValidator>()
    private val conversionService = mockk<ConversionService>()
    private val nftCollectionApi = mockk<NftCollectionControllerApi>()
    private val lazyNft = mockk<LazyNft>()

    private val lazyAssetValidator = LazyAssetValidator(delegate, nftCollectionApi, conversionService)
    
    @BeforeEach
    fun setup() {
        clearMocks(delegate, conversionService, lazyNft, nftCollectionApi)
        every { conversionService.convert(any(), eq(LazyNft::class.java)) } returns lazyNft
    }

    @Test
    fun `should validate lazy asset`() = runBlocking {
        val creator = Address.apply("0x25646b08d9796ceda5fb8ce0105a51820740c049")
        val token = AddressFactory.create()
        val tokenId = EthUInt256.of("0x25646b08d9796ceda5fb8ce0105a51820740c04900000000000000000000000a")

        val lazyAsset = mockk<Erc721AssetType>()

        val collection = mockk<NftCollectionDto> {
            every { features } returns listOf(NftCollectionDto.Features.MINT_AND_TRANSFER)
        }

        every { lazyNft.token } returns token
        every { lazyNft.tokenId } returns tokenId.value
        every { lazyNft.creators } returns listOf(Part(creator, 10))
        every { nftCollectionApi.getNftCollectionById(eq(token.hex())) } returns Mono.just(collection)
        coEvery { delegate.validate(eq(lazyNft)) } returns ValidationResult.Valid

        lazyAssetValidator.validate(lazyAsset, "take")

        coVerify(exactly = 1) { delegate.validate(eq(lazyNft)) }
        coVerify(exactly = 1) { conversionService.convert(eq(lazyAsset), eq(LazyNft::class.java)) }
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
        val tokenId = AddressFactory.create()
        val lazyAsset = mockk<AssetType>()

        val collection = mockk<NftCollectionDto> {
            every { features } returns emptyList()
        }

        every { lazyNft.token } returns tokenId
        every { nftCollectionApi.getNftCollectionById(eq(tokenId.hex())) } returns Mono.just(collection)
        coEvery { delegate.validate(eq(lazyNft)) } returns invalidResult

        assertThrows<OrderUpdateException> {
            runBlocking {
                lazyAssetValidator.validate(lazyAsset, "take")
            }
        }

        coVerify(exactly = 1) { delegate.validate(eq(lazyNft)) }
        coVerify(exactly = 1) { conversionService.convert(eq(lazyAsset), eq(LazyNft::class.java)) }
    }

    @Test
    fun `should throw exception on invalid tokenId`() = runBlocking {
        val token = AddressFactory.create()
        val tokenId = BigInteger.ONE
        val lazyAsset = mockk<AssetType>()

        val collection = mockk<NftCollectionDto> {
            every { features } returns listOf(NftCollectionDto.Features.MINT_AND_TRANSFER)
        }

        every { lazyNft.token } returns token
        every { lazyNft.tokenId } returns tokenId
        every { lazyNft.creators } returns listOf(Part(AddressFactory.create(), 10))
        every { nftCollectionApi.getNftCollectionById(eq(token.hex())) } returns Mono.just(collection)

        assertThrows<OrderUpdateException> {
            runBlocking {
                lazyAssetValidator.validate(lazyAsset, "take")
            }
        }

        coVerify(exactly = 0) { delegate.validate(eq(lazyNft)) }
        coVerify(exactly = 1) { conversionService.convert(eq(lazyAsset), eq(LazyNft::class.java)) }
    }
}
