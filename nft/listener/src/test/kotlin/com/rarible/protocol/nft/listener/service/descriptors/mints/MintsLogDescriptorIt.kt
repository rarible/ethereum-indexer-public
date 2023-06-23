package com.rarible.protocol.nft.listener.service.descriptors.mints

import com.rarible.core.test.wait.Wait.waitAssert
import com.rarible.protocol.contracts.erc721.mint.TestMintContract
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.util.stream.Stream

@IntegrationTest
class MintsLogDescriptorIt : AbstractIntegrationTest() {

    @BeforeEach
    fun enableFlag() {
        nftIndexerProperties.featureFlags.validateCreatorByTransactionSender = true
    }

    @AfterEach
    fun disableFlag() {
        nftIndexerProperties.featureFlags.validateCreatorByTransactionSender = false
    }

    companion object {
        @JvmStatic
        fun data() = Stream.of(
            Arguments.of(TokenStandard.ERC721, 1, 1, 1),
            Arguments.of(TokenStandard.ERC721, 5, 5, 1),
            Arguments.of(TokenStandard.ERC721, 5, 4, 1),
            Arguments.of(TokenStandard.ERC721, 0, 1, null),
            Arguments.of(TokenStandard.ERC1155, 1, 1, 1),
            Arguments.of(TokenStandard.ERC1155, 5, 5, 1),
            Arguments.of(TokenStandard.ERC1155, 0, 1, null),
        )
    }

    @ParameterizedTest
    @MethodSource("data")
    fun mintPrice(
        standard: TokenStandard,
        value: Int, // size of value in tx
        count: Int, // count of minted items
        mintPrice: Int?
    ) = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        depositInitialBalance(userSender.from(), value.toBigInteger())
        val token = TestMintContract.deployAndWait(userSender, poller).awaitFirst()
        tokenRepository.save(Token(
            id = token.address(),
            name = "Test",
            standard = standard
        )).awaitFirstOrNull()

        token
            .mintSomething(count.toBigInteger())
            .withValue(value.toBigInteger())
            .execute()
            .verifySuccess()

        waitAssert {
            val transfers = nftItemHistoryRepository
                .findItemsHistory(token = token.address())
                .collectList().awaitFirst()
            assertTrue(transfers.size > 0)
            val mint = transfers.first().item as ItemTransfer
            assertEquals(mintPrice?.toBigInteger(), mint.mintPrice)
        }
    }

    private suspend fun depositInitialBalance(to: Address, amount: BigInteger) {
        val coinBaseWalletPrivateKey = BigInteger(
            Numeric.hexStringToByteArray("00120de4b1518cf1f16dc1b02f6b4a8ac29e870174cb1d8575f578480930250a")
        )
        val (coinBaseAddress, coinBaseSender) = newSender(coinBaseWalletPrivateKey)
        coinBaseSender.sendTransaction(
            Transaction(
                to,
                coinBaseAddress,
                BigInteger.valueOf(8000000),
                BigInteger.ZERO,
                amount,
                Binary(ByteArray(1)),
                null
            )
        ).verifySuccess()
    }
}
