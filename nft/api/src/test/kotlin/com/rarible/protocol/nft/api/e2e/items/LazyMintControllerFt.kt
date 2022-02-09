package com.rarible.protocol.nft.api.e2e.items

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.contracts.erc1155.rarible.ERC1155Rarible
import com.rarible.protocol.contracts.erc1155.rarible.user.ERC1155RaribleUser
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.contracts.erc721.rarible.user.ERC721RaribleUserMinimal
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createAddress
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.api.e2e.data.randomItemMeta
import com.rarible.protocol.nft.core.converters.dto.NftItemMetaDtoConverter
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import io.daonomic.rpc.domain.Binary
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

@End2EndTest
class LazyMintControllerFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @Autowired
    private lateinit var lazyNftValidator: LazyNftValidator

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var nftItemMetaDtoConverter: NftItemMetaDtoConverter

    private val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

    private lateinit var creatorSender: MonoTransactionSender

    @BeforeEach
    fun before() = runBlocking<Unit> {
        coEvery { lazyNftValidator.validate(any()) } returns ValidationResult.Valid
        creatorSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            { Mono.just(BigInteger.ZERO) }
        )

        coEvery { mockItemMetaResolver.resolveItemMeta(any()) } returns null
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should any user mints ERC721`(version: ReduceVersion) = withReducer(version) {
        val contract = ERC721Rarible.deployAndWait(creatorSender, poller).awaitSingle()
        contract.__ERC721Rarible_init("Test", "TestSymbol", "BASE", "URI").execute().verifySuccess()
        val creator = randomAddress()
        val tokenId = EthUInt256.of("0x${scalether.util.Hex.to(creator.bytes())}00000000000000000000006B")

        val itemMeta = randomItemMeta()
        val itemId = ItemId(contract.address(), tokenId)
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns itemMeta
        val lazyItemDto = createLazyErc721Dto().copy(
            contract = contract.address(),
            tokenId = tokenId.value,
            creators = listOf(PartDto(creator, 10000))
        )

        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()
        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()
        checkItemDto(lazyItemDto, itemDto)

        assertThat(itemDto.meta).isEqualTo(nftItemMetaDtoConverter.convert(itemMeta, itemId.decimalStringValue))
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should only owner mints ERC721User`(version: ReduceVersion) = withReducer(version) {
        val contract = ERC721RaribleUserMinimal.deployAndWait(creatorSender, poller).awaitSingle()
        contract.__ERC721RaribleUser_init("Test", "TestSymbol", "BASE", "URI", emptyArray()).execute().verifySuccess()
        val creator = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        val tokenId = EthUInt256.of("0x${scalether.util.Hex.to(creator.bytes())}00000000000000000000006B")

        val itemMeta = randomItemMeta()
        val itemId = ItemId(contract.address(), tokenId)
        val ownershipId = OwnershipId(contract.address(), tokenId, creator)
        val lazyItemDto = createLazyErc721Dto().copy(
            contract = contract.address(),
            tokenId = tokenId.value,
            creators = listOf(PartDto(creator, 10000))
        )

        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()
        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()
        val ownershipDto = nftOwnershipApiClient.getNftOwnershipById(ownershipId.decimalStringValue, false).awaitFirst()
        checkItemDto(lazyItemDto, itemDto)
        checkOwnershipDto(lazyItemDto, ownershipDto)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `shouldn't random user mints ERC721User`(version: ReduceVersion) = withReducer(version) {
        val contract = ERC721RaribleUserMinimal.deployAndWait(creatorSender, poller).awaitSingle()
        contract.__ERC721RaribleUser_init("Test", "TestSymbol", "BASE", "URI", emptyArray()).execute().verifySuccess()
        val creator = randomAddress()
        val tokenId = EthUInt256.of("0x${scalether.util.Hex.to(creator.bytes())}00000000000000000000006B")

        val itemMeta = randomItemMeta()
        val itemId = ItemId(contract.address(), tokenId)
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns itemMeta
        val lazyItemDto = createLazyErc721Dto().copy(
            contract = contract.address(),
            tokenId = tokenId.value,
            creators = listOf(PartDto(creator, 10000))
        )

        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()
        assertThatThrownBy {
            runBlocking { nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst() }
        }
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should only owner mints ERC1155User`(version: ReduceVersion) = withReducer(version) {
        val contract = ERC1155RaribleUser.deployAndWait(creatorSender, poller).awaitSingle()
        contract.__ERC1155RaribleUser_init("Test", "TestSymbol", "BASE", "URI", emptyArray()).execute().verifySuccess()
        val creator = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        val tokenId = EthUInt256.of("0x${scalether.util.Hex.to(creator.bytes())}00000000000000000000006B")

        val itemMeta = randomItemMeta()
        val itemId = ItemId(contract.address(), tokenId)
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns itemMeta
        val lazyItemDto = createLazyErc1155Dto().copy(
            contract = contract.address(),
            tokenId = tokenId.value,
            creators = listOf(PartDto(creator, 10000))
        )

        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()
        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()
        checkItemDto(lazyItemDto, itemDto)
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should any user mints ERC1155`(version: ReduceVersion) = withReducer(version) {
        val contract = ERC1155Rarible.deployAndWait(creatorSender, poller).awaitSingle()
        contract.__ERC1155Rarible_init("Test", "TestSymbol", "BASE", "URI").execute().verifySuccess()
        val creator = randomAddress()
        val tokenId = EthUInt256.of("0x${scalether.util.Hex.to(creator.bytes())}00000000000000000000006B")

        val itemMeta = randomItemMeta()
        val itemId = ItemId(contract.address(), tokenId)
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns itemMeta
        val lazyItemDto = createLazyErc1155Dto().copy(
            contract = contract.address(),
            tokenId = tokenId.value,
            creators = listOf(PartDto(creator, 10000))
        )

        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()
        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()
        checkItemDto(lazyItemDto, itemDto)
    }

    @Test
    fun `shouldn't random user mints ERC1155User`() = runBlocking<Unit> {
        val contract = ERC1155RaribleUser.deployAndWait(creatorSender, poller).awaitSingle()
        contract.__ERC1155RaribleUser_init("Test", "TestSymbol", "BASE", "URI", emptyArray()).execute().verifySuccess()
        val creator = randomAddress()
        val tokenId = EthUInt256.of("0x${scalether.util.Hex.to(creator.bytes())}00000000000000000000006B")

        val lazyItemDto = createLazyErc1155Dto().copy(
            contract = contract.address(),
            tokenId = tokenId.value,
            creators = listOf(PartDto(creator, 10000))
        )

        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()
        assertThatThrownBy {
            runBlocking { nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst() }
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

    private fun checkOwnershipDto(
        lazyItemDto: LazyNftDto,
        ownershipDto: NftOwnershipDto
    ) {
        assertThat(ownershipDto.owner).isEqualTo(lazyItemDto.creators.first().account)
        assertThat(ownershipDto.tokenId).isEqualTo(lazyItemDto.tokenId)
        assertThat(ownershipDto.contract).isEqualTo(lazyItemDto.contract)
        when (lazyItemDto) {
            is LazyErc1155Dto -> {
                assertThat(ownershipDto.value).isEqualTo(lazyItemDto.supply)
                assertThat(ownershipDto.lazyValue).isEqualTo(lazyItemDto.supply)
            }
            is LazyErc721Dto -> {
                assertThat(ownershipDto.value).isEqualTo(BigInteger.ONE)
                assertThat(ownershipDto.lazyValue).isEqualTo(BigInteger.ONE)
            }
        }
    }

    private suspend fun checkItemDto(
        lazyItemDto: LazyNftDto,
        itemDto: NftItemDto
    ) {
        val itemId = ItemId(lazyItemDto.contract, EthUInt256(lazyItemDto.tokenId))
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

        //TODO: Fix
        //assertThat(itemDto.royalties.size).isEqualTo(lazyItemDto.royalties.size)

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
        assertThat(lazyMint.uri).isEqualTo(lazyItemDto.uri)

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

    private fun createLazyErc721Dto(): LazyErc721Dto {
        val token = createAddress()
        val tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
        return LazyErc721Dto(
            contract = token,
            tokenId = tokenId.value,
            uri = "https://placeholder.com",
            creators = listOf(PartDto(AddressFactory.create(), 5000)),
            royalties = listOf(PartDto(AddressFactory.create(), 5000)),
            signatures = listOf(Binary.empty())
        )
    }

    private fun createLazyErc1155Dto(): LazyErc1155Dto {
        val token = createAddress()
        val tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))

        return LazyErc1155Dto(
            contract = token,
            tokenId = tokenId.value,
            uri = "https://placeholder.com",
            supply = BigInteger.TEN,
            creators = listOf(PartDto(AddressFactory.create(), 5000)),
            royalties = listOf(PartDto(AddressFactory.create(), 5000)),
            signatures = listOf(Binary.empty())
        )
    }
}
