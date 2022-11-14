package com.rarible.protocol.erc20.listener.service.descriptors.erc20

import com.rarible.contracts.test.weth.TestWETH9
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.contract.repository.ContractRepository
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.model.Erc20Deposit
import com.rarible.protocol.erc20.core.model.Erc20Withdrawal
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.erc20.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.request.Transaction
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.time.Duration

@IntegrationTest
@Disabled("Fix in PT-1654")
internal class DepositAndWithdrawalLogDescriptorTest : AbstractIntegrationTest() {
    @Autowired
    protected lateinit var historyRepository: Erc20TransferHistoryRepository

    @Autowired
    protected lateinit var contractRepository: ContractRepository

    @Test
    fun `should get deposit history events`() = runBlocking<Unit> {
        val walletPrivateKey = BigInteger(Numeric.hexStringToByteArray("00120de4b1518cf1f16dc1b02f6b4a8ac29e870174cb1d8575f578480930250a"))
        val walletSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            walletPrivateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        walletSender.sendTransaction(
            Transaction(
                userSender.from(),
                walletSender.from(),
                BigInteger.valueOf(8000000),
                BigInteger.ZERO,
                (EthUInt256.TEN * EthUInt256.TEN).value,
                Binary(ByteArray(1)),
                null
            )
        ).verifySuccess()

        val contract = TestWETH9.deployAndWait(userSender, poller).awaitFirst()

        val deposit = EthUInt256.TEN
        contract.deposit()
            .withValue(deposit.value)
            .withSender(userSender)
            .execute().verifySuccess()

        val withdraw = EthUInt256.ONE
        contract.withdraw(withdraw.value)
            .withSender(userSender)
            .execute().verifySuccess()

        Wait.waitAssert(Duration.ofSeconds(5)) {
            val ownerHistory = historyRepository.findOwnerLogEvents(contract.address(), userSender.from()).collectList().awaitFirst()
            assertThat(ownerHistory).hasSize(2)

            with(ownerHistory[0].history as Erc20Deposit) {
                assertThat(this.token).isEqualTo(contract.address())
                assertThat(this.owner).isEqualTo(userSender.from())
                assertThat(this.value.value).isEqualTo(deposit.value)
            }
            with(ownerHistory[1].history as Erc20Withdrawal) {
                assertThat(this.token).isEqualTo(contract.address())
                assertThat(this.owner).isEqualTo(userSender.from())
                assertThat(this.value.value).isEqualTo(withdraw.value)
            }
            val savedToken = contractRepository.findById(contract.address())
            assertThat(savedToken).isNotNull
        }
    }
}

