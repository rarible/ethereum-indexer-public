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
import com.rarible.protocol.order.api.data.createErc20Asset
import com.rarible.protocol.order.api.service.order.validation.validators.LazyAssetValidator
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.toOrderExactFields
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import com.rarible.protocol.order.core.model.Part as ModelPart

internal class LazyAssetValidatorTest {

    private val delegate = mockk<LazyNftValidator>()
    private val nftCollectionApi = mockk<NftCollectionControllerApi>()

    private val lazyAssetValidator = LazyAssetValidator(delegate, nftCollectionApi)

    @BeforeEach
    fun setup() {
        clearMocks(delegate, nftCollectionApi)
    }

    @Test
    fun `should validate orderVersion with make lazy asset`() {
        `should validate orderVersion with lazy asset`(isBid = false)
    }

    @Test
    fun `should validate orderVersion with take lazy asset`() {
        `should validate orderVersion with lazy asset`(isBid = true)
    }

    private fun `should validate orderVersion with lazy asset`(isBid: Boolean) = runBlocking {
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
        val nftAsset = Asset(lazyNft.toErc721LazyAssetType(), EthUInt256.ONE)
        val currency = createErc20Asset()

        val (make, take) = if (isBid) currency to nftAsset else nftAsset to currency
        val orderVersion = createOrderVersion().copy(make = make, take = take)

        val collection = mockk<NftCollectionDto> {
            every { features } returns listOf(NftCollectionDto.Features.MINT_AND_TRANSFER)
        }

        every { nftCollectionApi.getNftCollectionById(token.hex()) } returns Mono.just(collection)
        coEvery { delegate.validate(lazyNft) } returns ValidationResult.Valid

        lazyAssetValidator.validate(orderVersion.toOrderExactFields())

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
        val nftAsset = Asset(lazyNft.toErc721LazyAssetType(), EthUInt256.ONE)
        val orderVersion = createOrderVersion().copy(make = nftAsset, take = createErc20Asset())

        val collection = mockk<NftCollectionDto> {
            every { features } returns emptyList()
        }

        every { nftCollectionApi.getNftCollectionById(token.hex()) } returns Mono.just(collection)
        coEvery { delegate.validate(lazyNft) } returns invalidResult

        assertThrows<OrderUpdateException> {
            runBlocking {
                lazyAssetValidator.validate(orderVersion.toOrderExactFields())
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
        val nftAsset = Asset(lazyNft.toErc721LazyAssetType(), EthUInt256.ONE)
        val orderVersion = createOrderVersion().copy(make = nftAsset, take = createErc20Asset())

        val collection = mockk<NftCollectionDto> {
            every { features } returns listOf(NftCollectionDto.Features.MINT_AND_TRANSFER)
        }

        every { nftCollectionApi.getNftCollectionById(token.hex()) } returns Mono.just(collection)

        assertThrows<OrderUpdateException> {
            runBlocking {
                lazyAssetValidator.validate(orderVersion.toOrderExactFields())
            }
        }

        coVerify(exactly = 0) { delegate.validate(lazyNft) }
    }

    private fun LazyERC721.toErc721LazyAssetType(): Erc721LazyAssetType {
        return Erc721LazyAssetType(
            token = this.token,
            tokenId = EthUInt256.of(this.tokenId),
            uri = this.uri,
            creators = this.creators.map { ModelPart(it.account, EthUInt256.of(it.value)) },
            royalties = this.royalties.map { ModelPart(it.account, EthUInt256.of(it.value)) },
            signatures = this.signatures
        )
    }
}
