package com.rarible.protocol.nft.api.e2e.items

import com.ninjasquad.springmockk.MockkBean
import com.rarible.ethereum.common.toBinary
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.dto.BurnLazyNftFormDto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.api.controller.ItemController
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createAddress
import com.rarible.protocol.nft.api.e2e.data.createLazyItemProperties
import com.rarible.protocol.nft.api.e2e.data.createPartDto
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.core.service.item.ItemReduceServiceV1
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaService
import io.daonomic.rpc.domain.Binary
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import scalether.abi.Uint256Type
import scalether.domain.Address
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.Instant

@End2EndTest
class BurnLazyMintFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @Autowired
    private lateinit var nftItemHistoryRepository: NftItemHistoryRepository

    @MockkBean
    private lateinit var lazyNftValidator: LazyNftValidator

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var itemReduceService: ItemReduceService

    @Autowired
    private lateinit var itemMetaService: ItemMetaService

    @Test
    fun `should burn mint lazy item`() = runBlocking {
        val privateKey = BigInteger.valueOf(100)
        val creator = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        // the first 20 bytes must be equal to the creator
        val bs = Binary.apply(creator.bytes().plus(ByteArray(12)))
        val tokenId = Uint256Type.decode(bs, 0).value()

        val lazyItemDto = createNft(tokenId, listOf(PartDto(creator, 10000)))

        // lazy mint
        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()

        val itemProperties = createLazyItemProperties()
        val itemId = ItemId(lazyItemDto.contract, EthUInt256(lazyItemDto.tokenId))

        coEvery { mockItemPropertiesResolver.resolve(itemId) } returns itemProperties

        coEvery { lazyNftValidator.validate(any()) } returns ValidationResult.Valid

        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()
        assertThat(itemDto.id).isEqualTo(itemId.decimalStringValue)
        val lazyMint = lazyNftItemHistoryRepository.findLazyMintById(itemId).awaitFirst()
        assertEquals(tokenId, lazyMint.tokenId.value)

        assertThat(itemMetaService.getItemMetadata(itemId).properties).isEqualToIgnoringGivenFields(itemProperties, ItemProperties::rawJsonContent.name)

        // burn
        val msg = ItemController.BURN_MSG.format(tokenId)
        val signature = sign(privateKey, msg).toBinary()

        val lazyDto = BurnLazyNftFormDto(lazyItemDto.creators.map { it.account }, listOf(signature))
        nftItemApiClient.deleteLazyMintNftAsset("${lazyItemDto.contract}:${lazyItemDto.tokenId}", lazyDto)
            .awaitFirstOrNull()
        coVerify(exactly = 1) { mockItemPropertiesResolver.reset(itemId) }
        coEvery { mockItemPropertiesResolver.resolve(itemId) } returns null
        assertThat(itemMetaService.getItemMetadata(itemId)).isEqualTo(ItemMeta(ItemProperties.EMPTY, ContentMeta.EMPTY))

        val item = itemRepository.findById(itemId).awaitSingle()
        assertTrue(item.deleted)
    }

    @Test
    fun `should burn lazy item after minting`() = runBlocking {
        val privateKey = BigInteger.valueOf(100)
        val creator = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        // the first 20 bytes must be equal to the creator
        val bs = Binary.apply(creator.bytes().plus(ByteArray(12)))
        val tokenId = Uint256Type.decode(bs, 0).value()

        val lazyItemDto = createNft(tokenId, listOf(PartDto(creator, 10000)))

        // lazy mint
        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()

        val itemId = ItemId(lazyItemDto.contract, EthUInt256(lazyItemDto.tokenId))

        coEvery { lazyNftValidator.validate(any()) } returns ValidationResult.Valid

        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()
        val lazyMint = lazyNftItemHistoryRepository.findLazyMintById(itemId).awaitFirst()

        // minting
        val eventMint = ItemTransfer(
            from = Address.ZERO(),
            token = itemDto.contract,
            tokenId = EthUInt256.of(itemDto.tokenId),
            value = EthUInt256.ONE,
            owner = creator,
            date = Instant.now()
        )
        val logMint = LogEvent(
            data = eventMint,
            address = Address.ZERO(),
            topic = ItemReduceServiceV1.WORD_ZERO,
            transactionHash = ItemReduceServiceV1.WORD_ZERO,
            status = LogEventStatus.CONFIRMED,
            blockNumber = 2,
            logIndex = 2,
            index = 2,
            minorLogIndex = 0
        )
        nftItemHistoryRepository.save(logMint).awaitFirstOrNull()
        val eventCreator = ItemCreators(
            token = itemDto.contract,
            tokenId = EthUInt256.of(itemDto.tokenId),
            creators = lazyMint.creators,
            date = Instant.now()
        )
        val logCreator = LogEvent(
            data = eventCreator,
            address = Address.ZERO(),
            topic = ItemReduceServiceV1.WORD_ZERO,
            transactionHash = ItemReduceServiceV1.WORD_ZERO,
            status = LogEventStatus.CONFIRMED,
            blockNumber = 1,
            logIndex = 1,
            index = 1,
            minorLogIndex = 0
        )
        nftItemHistoryRepository.save(logCreator).awaitFirstOrNull()
        itemReduceService.onItemHistories(listOf(logMint, logCreator)).awaitFirstOrNull()

        // checking after minting
        val item = itemRepository.findById(itemId).awaitSingle()

        assertThat(item.owners.isNotEmpty() || item.ownerships.isNotEmpty())
        if (item.owners.isNotEmpty()) {
            assertEquals(creator, item.owners[0])
        }
        if (item.ownerships.isNotEmpty()) {
            assertThat(item.ownerships.keys.single()).isEqualTo(creator)
        }

        assertEquals(EthUInt256.ONE, item.supply)
        assertEquals(EthUInt256.ZERO, item.lazySupply)

        // burn
        val eventBurn = ItemTransfer(
            from = creator,
            token = itemDto.contract,
            tokenId = EthUInt256.of(itemDto.tokenId),
            value = EthUInt256.ONE,
            owner = Address.ZERO(),
            date = Instant.now()
        )
        val logBurn = LogEvent(
            data = eventBurn,
            address = Address.ZERO(),
            topic = ItemReduceServiceV1.WORD_ZERO,
            transactionHash = ItemReduceServiceV1.WORD_ZERO,
            status = LogEventStatus.CONFIRMED,
            blockNumber = 4,
            logIndex = Int.MAX_VALUE,
            index = 4,
            minorLogIndex = 0
        )
        nftItemHistoryRepository.save(logBurn).awaitFirstOrNull()
        itemReduceService.onItemHistories(listOf(logBurn)).awaitFirstOrNull()

        val deletedItem = itemRepository.findById(itemId).awaitSingle()
        assertTrue(deletedItem.deleted)
        assertEquals(EthUInt256.ZERO, deletedItem.supply)
        assertEquals(EthUInt256.ZERO, deletedItem.lazySupply)
    }

    @Test
    fun `should fail with wrong signature`() = runBlocking {
        val privateKey = BigInteger.valueOf(100)
        val creator = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        // the first 20 bytes must be equal to the creator
        val bs = Binary.apply(creator.bytes().plus(ByteArray(12)))
        val tokenId = Uint256Type.decode(bs, 0).value()

        val lazyItemDto = createNft(tokenId, listOf(PartDto(creator, 10000)))

        // lazy mint
        val token = createToken().copy(id = lazyItemDto.contract, features = setOf(TokenFeature.MINT_AND_TRANSFER))
        tokenRepository.save(token).awaitFirst()
        coEvery { lazyNftValidator.validate(any()) } returns ValidationResult.Valid
        nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()

        // burn with wrong signature
        val signature = sign(privateKey, "").toBinary()

        val lazyDto = BurnLazyNftFormDto(lazyItemDto.creators.map { it.account }, listOf(signature))
        val ex = assertThrows<WebClientResponseException> {
            nftItemApiClient.deleteLazyMintNftAsset("${lazyItemDto.contract}:${lazyItemDto.tokenId}", lazyDto)
                .block()
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    private fun createNft(tokenId: BigInteger, creators: List<PartDto>): LazyErc721Dto {
        val contract = createAddress()
        return LazyErc721Dto(
            contract = contract,
            tokenId = tokenId,
            uri = "https://placeholder.com",
            royalties = listOf(createPartDto()),
            creators = creators,
            signatures = listOf(Binary.empty())
        )
    }

    private fun sign(privateKey: BigInteger, message: String): Sign.SignatureData {
        return Sign.signMessage(
            addStart(message).bytes(),
            Sign.publicKeyFromPrivate(privateKey),
            privateKey
        )
    }

    private fun addStart(message: String): Binary {
        val resultMessage = START + message.length + message
        return Binary.apply(resultMessage.toByteArray(StandardCharsets.US_ASCII))
    }

    companion object {
        private const val START = "\u0019Ethereum Signed Message:\n"
    }
}
