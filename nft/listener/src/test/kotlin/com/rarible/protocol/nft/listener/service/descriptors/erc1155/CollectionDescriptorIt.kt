package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.erc1155.rarible.ERC1155Rarible
import com.rarible.protocol.contracts.erc1155.rarible.user.ERC1155RaribleUser
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature.APPROVE_FOR_ALL
import com.rarible.protocol.nft.core.model.TokenFeature.BURN
import com.rarible.protocol.nft.core.model.TokenFeature.MINT_AND_TRANSFER
import com.rarible.protocol.nft.core.model.TokenFeature.MINT_WITH_ADDRESS
import com.rarible.protocol.nft.core.model.TokenFeature.SECONDARY_SALE_FEES
import com.rarible.protocol.nft.core.model.TokenFeature.SET_URI_PREFIX
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.web3jold.crypto.Keys
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class CollectionDescriptorIt : AbstractIntegrationTest() {

    @Test
    fun `should get CreateERC1155RaribleUserEvent event`() = runBlocking<Unit> {
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
            val savedToken = tokenRepository.findById(token.address()).awaitFirstOrNull()
            assertThat(savedToken).isNotNull
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
                    isRaribleContract = true,
                    //Byte code MUST be always the same for test contract
                    byteCodeHash = Word.apply("0xc91d15681fef753028aa737fe7a8d5cb027936c63003656fce82b25c0ad3e084")
                ),
                Token::revertableEvents.name,
                Token::dbUpdatedAt.name
            )
        }
    }

    @Test
    fun `should get CreateERC1155RaribleEvent event`() = runBlocking<Unit> {
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
            val savedToken = tokenRepository.findById(token.address()).awaitFirstOrNull()
            assertThat(savedToken).isNotNull
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
                    isRaribleContract = true,
                    //Byte code MUST be always the same for test contract
                    byteCodeHash = Word.apply("0x4d5b9d151267956dfa723742471de92a7126f83f093ca8b22554a8c0220e5eba")
                ),
                Token::revertableEvents.name,
                Token::dbUpdatedAt.name
            )
        }
    }
}
