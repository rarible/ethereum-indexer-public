package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.contracts.erc721.rarible.user.ERC721RaribleUserMinimal
import com.rarible.protocol.contracts.erc721.v4.MintableOwnableToken
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.web3jold.crypto.Keys
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class CollectionDescriptorIt : AbstractIntegrationTest() {

    @Test
    fun convert() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        // todo move to test contracts
        val token = poller.waitForTransaction(
            MintableOwnableToken.deploy(
                userSender,
                "Test",
                "TST",
                "https://ipfs",
                "https://ipfs",
                Address.apply(RandomUtils.nextBytes(20))
            )
        ).awaitFirst()

        assertThat(token.success()).isTrue()

        Wait.waitAssert {
            val savedToken = tokenRepository.findById(token.contractAddress()).awaitFirstOrNull()
            assertThat(savedToken).isNotNull

            assertEquals(savedToken!!.status, ContractStatus.CONFIRMED)
            assertEquals(savedToken.name, "Test")
            assertEquals(savedToken.owner, address)
            assertEquals(savedToken.symbol, "TST")
        }
    }

    @Test
    fun `should get CreateERC721RaribleUserEvent event`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val token = ERC721RaribleUserMinimal.deployAndWait(userSender, poller).awaitFirst()
        token.__ERC721RaribleUser_init("Test", "TestSymbol", "BASE", "URI", arrayOf()).execute().verifySuccess()

        Wait.waitAssert {
            val savedToken = tokenRepository.findById(token.address()).awaitFirstOrNull()
            assertThat(savedToken).isNotNull

            assertEquals(savedToken!!.status, ContractStatus.CONFIRMED)
            assertEquals(savedToken.name, "Test")
            assertEquals(savedToken.owner, address)
            assertEquals(savedToken.symbol, "TestSymbol")
        }
    }

    @Test
    fun `should get CreateERC721RaribleEvent event`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val token = ERC721Rarible.deployAndWait(userSender, poller).awaitFirst()
        token.__ERC721Rarible_init("Test", "TestSymbol", "BASE", "URI").execute().verifySuccess()

        Wait.waitAssert {
            val savedToken = tokenRepository.findById(token.address()).awaitFirstOrNull()
            assertThat(savedToken).isNotNull

            assertEquals(savedToken!!.status, ContractStatus.CONFIRMED)
            assertEquals(savedToken.name, "Test")
            assertEquals(savedToken.owner, address)
            assertEquals(savedToken.symbol, "TestSymbol")
        }
    }
}
