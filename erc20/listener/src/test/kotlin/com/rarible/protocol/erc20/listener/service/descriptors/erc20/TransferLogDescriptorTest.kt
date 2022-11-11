package com.rarible.protocol.erc20.listener.service.descriptors.erc20

import com.rarible.contracts.test.erc20.TestERC20
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.contract.repository.ContractRepository
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.erc20.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.time.Duration

@IntegrationTest
@Disabled("Fix in PT-1654")
internal class TransferLogDescriptorTest : AbstractIntegrationTest() {
    @Autowired
    protected lateinit var historyRepository: Erc20TransferHistoryRepository

    @Autowired
    protected lateinit var contractRepository: ContractRepository

    @Test
    fun `should get transfer history events`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val recipient = randomAddress()
        val value = BigInteger.valueOf(10)

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        val token = TestERC20.deployAndWait(userSender, poller, "Test name", "test symbol").awaitFirst()

        val owner = userSender.from()
        token.mint(owner, value).execute().verifySuccess()

        token.transfer(recipient, value).execute().verifySuccess()

        Wait.waitAssert(Duration.ofSeconds(5)) {
            val ownerHistory = historyRepository.findOwnerLogEvents(token.address(), owner).collectList().awaitFirst()
            Assertions.assertThat(ownerHistory).hasSize(2)

            with(ownerHistory[0].history as Erc20IncomeTransfer ) {
                Assertions.assertThat(this.token).isEqualTo(token.address())
                Assertions.assertThat(this.owner).isEqualTo(owner)
                Assertions.assertThat(this.value.value).isEqualTo(value)
            }
            with(ownerHistory[1].history as Erc20OutcomeTransfer ) {
                Assertions.assertThat(this.token).isEqualTo(token.address())
                Assertions.assertThat(this.owner).isEqualTo(owner)
                Assertions.assertThat(this.value.value).isEqualTo(value)
            }

            val recipientHistory = historyRepository.findOwnerLogEvents(token.address(), recipient).collectList().awaitFirst()
            Assertions.assertThat(recipientHistory).hasSize(1)

            with(recipientHistory[0].history as Erc20IncomeTransfer ) {
                Assertions.assertThat(this.token).isEqualTo(token.address())
                Assertions.assertThat(this.owner).isEqualTo(recipient)
                Assertions.assertThat(this.value.value).isEqualTo(value)
            }

            val savedToken = contractRepository.findById(token.address())
            Assertions.assertThat(savedToken).isNotNull
        }
    }
}
