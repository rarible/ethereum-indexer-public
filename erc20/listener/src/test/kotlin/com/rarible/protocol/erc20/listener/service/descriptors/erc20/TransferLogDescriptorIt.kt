package com.rarible.protocol.erc20.listener.service.descriptors.erc20

import com.rarible.contracts.test.erc20.TestERC20
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.contract.repository.ContractRepository
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.test.AbstractIntegrationTest
import com.rarible.protocol.erc20.listener.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Duration

@IntegrationTest
internal class TransferLogDescriptorIt : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var historyRepository: Erc20TransferHistoryRepository

    @Autowired
    protected lateinit var contractRepository: ContractRepository

    @Test
    fun `should get transfer history events`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val recipient = randomAddress()
        val value = BigInteger.valueOf(10)

        val userSender = createSender(privateKey)
        val token = TestERC20.deployAndWait(userSender, poller, randomString(), randomString()).awaitFirst()

        val owner = userSender.from()

        token.mint(owner, value).execute().verifySuccess()
        token.transfer(recipient, value).execute().verifySuccess()

        Wait.waitAssert(timeout = Duration.ofSeconds(10)) {
            val ownerHistory = historyRepository.findOwnerLogEvents(
                token.address(),
                owner
            ).collectList().awaitFirst()

            assertThat(ownerHistory).hasSize(2)

            with(ownerHistory[0].history as Erc20IncomeTransfer) {
                assertThat(this.token).isEqualTo(token.address())
                assertThat(this.owner).isEqualTo(owner)
                assertThat(this.value.value).isEqualTo(value)
            }
            with(ownerHistory[1].history as Erc20OutcomeTransfer) {
                assertThat(this.token).isEqualTo(token.address())
                assertThat(this.owner).isEqualTo(owner)
                assertThat(this.value.value).isEqualTo(value)
            }

            val recipientHistory = historyRepository.findOwnerLogEvents(
                token.address(),
                recipient
            ).collectList().awaitFirst()

            assertThat(recipientHistory).hasSize(1)

            with(recipientHistory[0].history as Erc20IncomeTransfer) {
                assertThat(this.token).isEqualTo(token.address())
                assertThat(this.owner).isEqualTo(recipient)
                assertThat(this.value.value).isEqualTo(value)
            }

            val savedToken = contractRepository.findById(token.address())
            assertThat(savedToken).isNotNull
        }
    }
}
