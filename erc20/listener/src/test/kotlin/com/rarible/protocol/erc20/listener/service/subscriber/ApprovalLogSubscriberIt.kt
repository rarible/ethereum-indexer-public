package com.rarible.protocol.erc20.listener.service.subscriber

import com.rarible.contracts.test.erc20.TestERC20
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.contract.repository.ContractRepository
import com.rarible.protocol.erc20.core.model.Erc20TokenApproval
import com.rarible.protocol.erc20.core.repository.Erc20ApprovalHistoryRepository
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
internal class ApprovalLogSubscriberIt : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var historyRepository: Erc20ApprovalHistoryRepository

    @Autowired
    protected lateinit var contractRepository: ContractRepository

    @Test
    fun `should get approval history event`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

        val userSender = createSender(privateKey)
        val token = TestERC20.deployAndWait(userSender, poller, randomString(), randomString()).awaitFirst()

        val owner = userSender.from()
        val spender = randomAddress()

        val value = BigInteger.valueOf(10)

        token.approve(spender, value).execute().verifySuccess()

        Wait.waitAssert(timeout = Duration.ofSeconds(10)) {
            val history = historyRepository.findOwnerLogEvents(
                token.address(),
                owner
            ).collectList().awaitFirst()

            assertThat(history).hasSize(1)

            with(history[0].history as Erc20TokenApproval) {
                assertThat(this.spender).isEqualTo(spender)
                assertThat(this.owner).isEqualTo(owner)
                assertThat(this.value.value).isEqualTo(value)
            }

            val savedToken = contractRepository.findById(token.address())
            assertThat(savedToken).isNotNull
        }
    }
}
