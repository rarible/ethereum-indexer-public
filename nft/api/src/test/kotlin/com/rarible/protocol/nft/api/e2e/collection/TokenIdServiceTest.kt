package com.rarible.protocol.nft.api.e2e.collection

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties.OperatorProperties
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.api.service.colllection.CollectionService
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenIdRepository
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.core.service.token.meta.TokenMetaService
import io.mockk.CoFunctionAnswer
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3jold.crypto.Sign
import org.web3jold.utils.Numeric
import scalether.abi.Uint256Type
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.util.Hex
import java.math.BigInteger

class CollectionServiceIt {
    private val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
    private val publicKey = Sign.publicKeyFromPrivate(privateKey)

    private val mockOperator = OperatorProperties(Hex.prefixed(privateKey.toByteArray()))
    private val properties = mockk<NftIndexerApiProperties> {
        every { operator } returns mockOperator
        every { metaTimeout } returns 1
    }
    private val tokenRepository = mockk<TokenRepository>()
    private val tokenIdRepository = mockk<TokenIdRepository>()
    private val tokenService = mockk<TokenService>()
    private val tokenMetaService = mockk<TokenMetaService>()
    private val collectionService = CollectionService(
        properties,
        tokenService,
        tokenRepository,
        tokenIdRepository,
        tokenMetaService
    )

    @BeforeEach
    fun setup() {
        clearMocks(tokenRepository, tokenIdRepository)
    }

    @Test
    fun testGenerateWithMinter() = runBlocking<Unit> {
        val tokenAddress = AddressFactory.create()
        val token = createToken().copy(
            standard = TokenStandard.ERC1155,
            features = setOf(TokenFeature.MINT_AND_TRANSFER)
        )
        val maker = Address.apply("0x25646b08d9796ceda5fb8ce0105a51820740c049")

        coEvery { tokenIdRepository.generateTokenId(eq("$tokenAddress:$maker")) } returns 10L
        coEvery { tokenService.register(eq(tokenAddress)) } returns token

        val result = collectionService.generateId(tokenAddress, maker)

        assertThat(result.tokenId).isEqualTo(EthUInt256.of("0x25646b08d9796ceda5fb8ce0105a51820740c04900000000000000000000000a"))
    }

    @Test
    fun testGenerateSignWithTokenAddress() = runBlocking<Unit> {
        val tokenAddress = AddressFactory.create()

        val token = createToken().copy(
            id = tokenAddress,
            standard = TokenStandard.ERC1155,
            features = setOf(TokenFeature.MINT_WITH_ADDRESS, TokenFeature.MINT_AND_TRANSFER)
        )
        val maker = AddressFactory.create()

        val nextTokenId = BigInteger.valueOf(10)
        coEvery { tokenIdRepository.generateTokenId(eq("$tokenAddress:$maker")) } returns nextTokenId.longValueExact()
        coEvery { tokenService.register(eq(tokenAddress)) } returns token

        val result = collectionService.generateId(tokenAddress, maker)

        val sign = Sign.signMessage(tokenAddress.add(Uint256Type.encode(result.tokenId.value)).bytes(), publicKey, privateKey)

        assertThat(result.sign.r).isEqualTo(sign.r)
        assertThat(result.sign.s).isEqualTo(sign.s)
        assertThat(result.sign.v).isEqualTo(sign.v)
    }

    @Test
    fun testGenerateWithCollection() = runBlocking<Unit> {
        val tokenAddress = AddressFactory.create()

        val token = createToken().copy(
            id = tokenAddress,
            standard = TokenStandard.ERC1155,
            features = setOf()
        )
        val maker = AddressFactory.create()

        val nextTokenId = 10L
        coEvery { tokenIdRepository.generateTokenId(eq("$tokenAddress")) } returns nextTokenId
        coEvery { tokenService.register(eq(tokenAddress)) } returns token

        val result = collectionService.generateId(tokenAddress, maker)

        val sign = Sign.signMessage(Uint256Type.encode(result.tokenId.value).bytes(), publicKey, privateKey)

        assertThat(result.sign.r).isEqualTo(sign.r)
        assertThat(result.sign.s).isEqualTo(sign.s)
        assertThat(result.sign.v).isEqualTo(sign.v)
    }

    @Test
    fun testMetaTimeout() = runBlocking<Unit> {
        val token = createToken().copy(standard = TokenStandard.ERC721)

        every { tokenRepository.findByIds(any()) } returns flowOf(token)
        coEvery { tokenMetaService.get(any()) } answers CoFunctionAnswer {
            delay(10_000)
            TokenMeta(
                properties = TokenProperties(
                    name = "Bored ape",
                    description = null,
                    externalUri = null,
                    feeRecipient = null,
                    sellerFeeBasisPoints = null
                )
            )
        }

        val tokenWithMeta = collectionService.get(listOf(token.id))[0]

        assertThat(tokenWithMeta.id).isEqualTo(token.id)
    }
}
