package com.rarible.protocol.nft.api.e2e.items

import com.ninjasquad.springmockk.MockkBean
import com.rarible.ethereum.common.toBinary
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.dto.BurnLazyNftFormDto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.api.controller.ItemController
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createAddress
import com.rarible.protocol.nft.api.e2e.data.createPartDto
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import io.daonomic.rpc.domain.Binary
import io.mockk.coEvery
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
import java.util.*

@End2EndTest
class BurnLazyMintFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @MockkBean
    private lateinit var lazyNftValidator: LazyNftValidator

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

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

        val itemId = ItemId(lazyItemDto.contract, EthUInt256(lazyItemDto.tokenId))

        coEvery { lazyNftValidator.validate(any()) } returns ValidationResult.Valid

        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()
        assertThat(itemDto.id).isEqualTo(itemId.decimalStringValue)
        val lazyMint = lazyNftItemHistoryRepository.findById(itemId).awaitFirst()
        assertEquals(tokenId, lazyMint.tokenId.value)

        // burn
        val msg = ItemController.BURN_MSG.format(tokenId)
        val signature = sign(privateKey, msg).toBinary()

        val lazyDto = BurnLazyNftFormDto(lazyItemDto.creators.map { it.account }, listOf(signature))
        nftItemApiClient.deleteLazyMintNftAsset("${lazyItemDto.contract}:${lazyItemDto.tokenId}", lazyDto).awaitFirstOrNull()

        val item = itemRepository.findById(itemId).awaitSingle()
        assertTrue(item.deleted)
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
        val itemDto = nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitFirst()

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
            uri = UUID.randomUUID().toString(),
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
