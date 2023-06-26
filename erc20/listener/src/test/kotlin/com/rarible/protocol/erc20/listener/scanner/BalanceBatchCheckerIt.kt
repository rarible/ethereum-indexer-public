package com.rarible.protocol.erc20.listener.scanner

import com.rarible.core.test.wait.BlockingWait.waitAssert
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.erc20.test.SimpleERC20
import com.rarible.protocol.erc20.core.metric.CheckerMetrics
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import com.rarible.protocol.erc20.listener.test.AbstractIntegrationTest
import com.rarible.protocol.erc20.listener.test.IntegrationTest
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3jold.utils.Numeric

// This test is flaky due to kafka events from other tests can change metrics
// Better to run these test separately
@Disabled
@FlowPreview
@IntegrationTest
class BalanceBatchCheckerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @Autowired
    lateinit var blockchain: Blockchain

    @Autowired
    lateinit var erc20BalanceService: Erc20BalanceService

    val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        meterRegistry.clear()
    }

    @Test
    fun `should check balance - ok`() = runBlocking<Unit> {
        val sender = createSender(privateKey)
        val walletOwner = sender.from()
        val erc20 = SimpleERC20.deployAndWait(sender, poller).awaitFirst()
        erc20.mint(walletOwner, 10.toBigInteger()).withSender(sender).execute().verifySuccess()

        // transfer to random address
//        erc20.approve(walletOwner, 9.toBigInteger()).withSender(sender).execute().verifySuccess()
//        erc20.transferFrom(walletOwner, AddressFactory.create(), 3.toBigInteger()).withSender(sender).execute().verifySuccess()

        meterRegistry.clear() // metric clean
        waitAssert {
            assertThat(counter(CheckerMetrics.BALANCE_CHECK)).isEqualTo(1.0)
            assertThat(counter(CheckerMetrics.BALANCE_INCOMING)).isEqualTo(1.0)
            assertThat(counter(CheckerMetrics.BALANCE_INVALID)).isEqualTo(0.0)
        }
    }

    @Test
    fun `should check balance - fail`() = runBlocking<Unit> {
        val sender = createSender(privateKey)
        val walletOwner = sender.from()
        val erc20 = SimpleERC20.deployAndWait(sender, poller).awaitFirst()
        val blockNumber = ethereum.ethBlockNumber().awaitFirst().toLong()

        meterRegistry.clear() // metric clean
        erc20BalanceService.update(randomBalance(
            token = erc20.address(),
            owner = walletOwner,
            balance = EthUInt256.of(2),
            blockNumber = blockNumber
        ))

        waitAssert {
            assertThat(counter(CheckerMetrics.BALANCE_INVALID)).isEqualTo(1.0)
        }
    }

    private fun counter(name: String): Double {
        return meterRegistry.counter(name, listOf(ImmutableTag("blockchain", blockchain.name.lowercase()))).count()
    }
}
