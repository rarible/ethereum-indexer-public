package com.rarible.protocol.erc20.listener.task

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.core.repository.data.randomErc20Deposit
import com.rarible.protocol.erc20.core.repository.data.randomErc20IncomeTransfer
import com.rarible.protocol.erc20.core.repository.data.randomErc20OutcomeTransfer
import com.rarible.protocol.erc20.core.repository.data.randomLogEvent
import com.rarible.protocol.erc20.listener.test.AbstractIntegrationTest
import com.rarible.protocol.erc20.listener.test.IntegrationTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
class Erc20BalanceReduceTaskHandlerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var historyRepository: Erc20TransferHistoryRepository

    @Autowired
    lateinit var balanceRepository: Erc20BalanceRepository

    @Autowired
    lateinit var handler: Erc20BalanceReduceTaskHandler

    @Test
    fun `reduce token balances - from beginning`() = runBlocking<Unit> {
        val token1 = Address.apply("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2") // weth
        val token2 = randomAddress()
        val owner1 = randomAddress()
        val owner2 = randomAddress()

        // Should be reduced
        val logEvent1 = randomLogEvent(randomErc20Deposit(token1, owner1, BigInteger.TEN), blockNumber = 1)
        val logEvent2 = randomLogEvent(randomErc20OutcomeTransfer(token1, owner1, BigInteger.ONE), blockNumber = 2)
        val logEvent3 = randomLogEvent(randomErc20IncomeTransfer(token1, owner2, BigInteger.TEN), blockNumber = 2)
        val logEvent4 = randomLogEvent(randomErc20OutcomeTransfer(token1, owner2, BigInteger.ONE), blockNumber = 3)

        // Should be skipped
        val logEvent5 = randomLogEvent(randomErc20Deposit(token2, owner1), blockNumber = 1)

        saveAll(logEvent1, logEvent2, logEvent3, logEvent4, logEvent5)

        handler.runLongTask(null, token1.prefixed()).collect()

        val balance1 = balanceRepository.get(BalanceId(token1, owner1))!!
        val balance2 = balanceRepository.get(BalanceId(token1, owner2))!!
        val balance3 = balanceRepository.get(BalanceId(token2, owner1))

        assertThat(balance1.balance).isEqualTo(EthUInt256.of(9))
        assertThat(balance2.balance).isEqualTo(EthUInt256.of(9))
        assertThat(balance3).isNull()
    }

    @Test
    fun `reduce token balances - from middle`() = runBlocking<Unit> {
        val token = randomAddress()
        val owner1 = Address.TWO()
        val owner2 = Address.THREE()

        // Should be skipped due to task state
        val logEvent1 = randomLogEvent(randomErc20Deposit(token, owner1, BigInteger.TEN), blockNumber = 1)
        val logEvent2 = randomLogEvent(randomErc20OutcomeTransfer(token, owner1, BigInteger.ONE), blockNumber = 2)
        // Should be reduced
        val logEvent3 = randomLogEvent(randomErc20IncomeTransfer(token, owner2, BigInteger.TEN), blockNumber = 2)
        val logEvent4 = randomLogEvent(randomErc20OutcomeTransfer(token, owner2, BigInteger.ONE), blockNumber = 3)

        saveAll(logEvent1, logEvent2, logEvent3, logEvent4)

        handler.runLongTask(BalanceId(token, owner1), token.prefixed()).collect()

        val balance1 = balanceRepository.get(BalanceId(token, owner1))
        val balance2 = balanceRepository.get(BalanceId(token, owner2))!!

        assertThat(balance1).isNull()
        assertThat(balance2.balance).isEqualTo(EthUInt256.of(9))
    }

    private suspend fun saveAll(vararg logs: ReversedEthereumLogRecord) {
        logs.forEach { log ->
            historyRepository.save(log).awaitFirst()
        }
    }
}
