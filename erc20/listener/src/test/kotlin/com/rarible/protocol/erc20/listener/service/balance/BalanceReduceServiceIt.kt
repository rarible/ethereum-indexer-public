package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.test.AbstractIntegrationTest
import com.rarible.protocol.erc20.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.util.Date

@FlowPreview
@IntegrationTest
internal class BalanceReduceServiceIt : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var balanceReduceService: Erc20BalanceReduceService

    @Autowired
    protected lateinit var historyRepository: Erc20TransferHistoryRepository

    @Autowired
    protected lateinit var erc20BalanceRepository: Erc20BalanceRepository

    @Test
    fun `should calculate balance for not existed balance`() = runBlocking<Unit> {
        val walletToken = randomAddress()
        val walletOwner = randomAddress()
        val balanceId = BalanceId(walletToken, walletOwner)

        val createdDate = Date(nowMillis().minusSeconds(30).toEpochMilli())
        val otherActionsDate = Date(nowMillis().minusSeconds(20).toEpochMilli())
        val finalUpdateDate = Date(nowMillis().minusSeconds(10).toEpochMilli())

        prepareStorage(
            walletToken,
            Erc20IncomeTransfer(owner = walletOwner, token = walletToken, date = createdDate, value = EthUInt256.of(3)),
            Erc20IncomeTransfer(
                owner = walletOwner, token = walletToken, date = otherActionsDate, value = EthUInt256.of(7)
            ),
            Erc20OutcomeTransfer(
                owner = walletOwner, token = walletToken, date = otherActionsDate, value = EthUInt256.of(4)
            ),
            Erc20OutcomeTransfer(
                owner = walletOwner, token = walletToken, date = finalUpdateDate, value = EthUInt256.of(5)
            )
        )

        balanceReduceService.update(walletToken, walletOwner)

        val balance = erc20BalanceRepository.get(balanceId)!!
        assertThat(balance.balance).isEqualTo(EthUInt256.ONE)
        assertThat(balance.lastUpdatedAt!!.toEpochMilli()).isEqualTo(finalUpdateDate.time)
        assertThat(balance.createdAt!!.toEpochMilli()).isEqualTo(createdDate.time)
        assertThat(balance.blockNumber).isGreaterThan(0L)
    }

    @Test
    fun `shouldn't calculate balance for ignored owner`() = runBlocking<Unit> {
        val walletToken = randomAddress()
        val walletOwner = Address.apply("0x1a250d5630b4cf539739df2c5dacb4c659f2488d")
        val balanceId = BalanceId(walletToken, walletOwner)

        prepareStorage(
            walletToken,
            Erc20IncomeTransfer(owner = walletOwner, token = walletToken, date = Date(), value = EthUInt256.of(3)),
        )

        balanceReduceService.update(walletToken, walletOwner)

        val balance = erc20BalanceRepository.get(balanceId)
        assertThat(balance).isNull()
    }

    private suspend fun prepareStorage(token: Address, vararg histories: Erc20TokenHistory) {
        histories.forEachIndexed { index, history ->
            historyRepository.save(
                ReversedEthereumLogRecord(
                    id = randomString(),
                    data = history,
                    address = token,
                    topic = word(),
                    transactionHash = randomWord(),
                    status = EthereumLogStatus.CONFIRMED,
                    blockNumber = 1,
                    logIndex = 0,
                    minorLogIndex = index,
                    index = 0
                )
            ).awaitFirst()
        }
    }

    private fun word(): Word = Word(RandomUtils.nextBytes(32))
}
