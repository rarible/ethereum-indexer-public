package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.erc1155.rarible.ERC1155Rarible
import com.rarible.protocol.contracts.erc1155.rarible.user.ERC1155RaribleUser
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature.*
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class CollectionDescriptorTest : AbstractIntegrationTest() {

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should get CreateERC1155RaribleUserEvent event`(version: ReduceVersion) = withReducer(version) {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val token = ERC1155RaribleUser.deployAndWait(userSender, poller).awaitFirst()
        token.__ERC1155RaribleUser_init("Test", "TestSymbol", "BASE", "URI", arrayOf()).execute().verifySuccess()

        Wait.waitAssert {
            assertThat(tokenRepository.count().awaitFirst()).isEqualTo(1)
            val savedToken = tokenRepository.findById(token.address()).awaitFirstOrNull()
            assertThat(savedToken).isEqualToIgnoringGivenFields(
                Token(
                    id = token.address(),
                    owner = address,
                    name = "Test",
                    symbol = "TestSymbol",
                    status = ContractStatus.CONFIRMED,
                    features = setOf(
                        APPROVE_FOR_ALL,
                        SET_URI_PREFIX,
                        BURN,
                        MINT_WITH_ADDRESS,
                        SECONDARY_SALE_FEES,
                        MINT_AND_TRANSFER
                    ),
                    lastEventId = savedToken?.lastEventId,
                    standard = TokenStandard.ERC1155,
                    version = savedToken?.version,
                    isRaribleContract = true
                ),
                Token::revertableEvents.name
            )
        }
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should get CreateERC1155RaribleEvent event`(version: ReduceVersion) = withReducer(version) {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val token = ERC1155Rarible.deployAndWait(userSender, poller).awaitFirst()
        token.__ERC1155Rarible_init("Test", "TestSymbol", "BASE", "URI").execute().verifySuccess()

        Wait.waitAssert {
            assertThat(tokenRepository.count().awaitFirst()).isEqualTo(1)
            val savedToken = tokenRepository.findById(token.address()).awaitFirstOrNull()
            assertThat(savedToken).isEqualToIgnoringGivenFields(
                Token(
                    id = token.address(),
                    owner = address,
                    name = "Test",
                    symbol = "TestSymbol",
                    status = ContractStatus.CONFIRMED,
                    features = setOf(
                        APPROVE_FOR_ALL,
                        SET_URI_PREFIX,
                        BURN,
                        MINT_WITH_ADDRESS,
                        SECONDARY_SALE_FEES,
                        MINT_AND_TRANSFER
                    ),
                    lastEventId = savedToken?.lastEventId,
                    standard = TokenStandard.ERC1155,
                    version = savedToken?.version,
                    isRaribleContract = true
                ),
                Token::revertableEvents.name
            )
        }
    }
}
