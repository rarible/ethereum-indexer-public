package com.rarible.protocol.nft.listener.service.checker

import com.rarible.core.test.wait.BlockingWait
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.erc1155.rarible.ERC1155Rarible
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.nft.core.metric.BaseMetrics
import com.rarible.protocol.nft.core.metric.CheckerMetrics
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3jold.crypto.Keys
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scala.Tuple2
import scala.Tuple5
import scala.Tuple6
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.util.Hex
import java.math.BigInteger

// This test is flaky due to kafka events from other tests can change metrics
// Better to run these test manually one by one
@Disabled
@IntegrationTest
class OwnershipCheckerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @BeforeEach
    fun clean() = runBlocking<Unit> {
        meterRegistry.clear()
    }

    @Test
    fun `should check erc721`() = runBlocking<Unit> {
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

        val tokenId = EthUInt256.of("0x${Hex.to(address.bytes())}00000000000000000000006B")

        val mintData = Tuple5(
            tokenId.value,
            "TestUri",
            arrayOf(
                Tuple2(
                    address,
                    BigInteger.valueOf(10000)
                )
            ),
            emptyArray<Tuple2<Address, BigInteger>>(),
            arrayOf(ByteArray(0))
        )

        val transferTo = AddressFactory.create()
        token.mintAndTransfer(mintData, transferTo)
            .withSender(userSender)
            .execute()
            .verifySuccess()

        BlockingWait.waitAssert {
            assertThat(counter(CheckerMetrics.SUCCESS_TAG)).isEqualTo(1.0)
            assertThat(counter(CheckerMetrics.FAIL_TAG)).isEqualTo(0.0)
            assertThat(counter(CheckerMetrics.SKIPPED_TAG)).isEqualTo(0.0)
        }
    }

    @Test
    fun `should check erc1155`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val token = ERC1155Rarible.deployAndWait(userSender, poller).awaitFirst()
        token.__ERC1155Rarible_init("Test", "TestSymbol", "BASE", "URI").execute().verifySuccess()

        val tokenId = EthUInt256.of("0x${Hex.to(address.bytes())}00000000000000000000006B")

        val mintData = Tuple6(
            tokenId.value,
            "TestUri",
            BigInteger.ONE,
            arrayOf(
                Tuple2(
                    address,
                    BigInteger.valueOf(10000)
                )
            ),
            emptyArray<Tuple2<Address, BigInteger>>(),
            arrayOf(ByteArray(0))
        )

        val transferTo = AddressFactory.create()
        token.mintAndTransfer(mintData, transferTo, BigInteger.ONE)
            .withSender(userSender)
            .execute()
            .verifySuccess()

        BlockingWait.waitAssert {
            assertThat(counter(CheckerMetrics.SUCCESS_TAG)).isEqualTo(1.0)
            assertThat(counter(CheckerMetrics.FAIL_TAG)).isEqualTo(0.0)
            assertThat(counter(CheckerMetrics.SKIPPED_TAG)).isEqualTo(0.0)
        }
    }

    private fun counter(name: String): Double {
        return meterRegistry.counter(CheckerMetrics.OWNERSHIPS_CHECKED, listOf(
            ImmutableTag(BaseMetrics.BLOCKCHAIN, Blockchain.ETHEREUM.value.lowercase()),
            ImmutableTag(BaseMetrics.STATUS, name))).count()
    }
}
