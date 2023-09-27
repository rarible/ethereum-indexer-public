package com.rarible.protocol.nft.listener.service.descriptors.mints

import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class TransferLogsPostProcessorIt : AbstractIntegrationTest() {

    @BeforeEach
    fun before() {
        featureFlags.detectScamTokenThreshold = 10
    }

    @Test
    fun `detect scam`(): Unit = runBlocking {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val token = TestERC1155.deployAndWait(userSender, poller, "TEST").awaitFirst()
        val itemId = ItemId(token.address(), EthUInt256(BigInteger.ONE))
        val receivers = (1..featureFlags.detectScamTokenThreshold).map { randomAddress() }
        token.mint(userSender.from(), itemId.tokenId.value, BigInteger.ONE, ByteArray(0))

    }
}
