package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.log
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.blur.BlurEventConverter
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.HistorySubscriber
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

abstract class AbstractBlurDescriptorTest {
    protected val contractsProvider = mockk<ContractsProvider> {
        every { blurV1() } returns listOf(randomAddress())
    }
    protected val blurEventConverter = mockk<BlurEventConverter>()
    protected val autoReduceService = mockk<AutoReduceService>()

    protected suspend fun <T : EventData> checkConversion(
        subscriber: HistorySubscriber<T>,
        eventData: List<T>,
        mockk: (log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant) -> Unit
    ) {
        val index = 1
        val totalLogs = 10
        val date = Instant.ofEpochSecond(1000)

        val log = log(listOf(Word.apply(randomWord())))
        val transaction = mockk<Transaction> {
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
        }
        val block = mockk<EthereumBlockchainBlock>() {
            every { timestamp } returns date.epochSecond
        }
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = index,
            total = totalLogs,
        )
        mockk(log, transaction, index, totalLogs, date)

        val records = subscriber.getEthereumEventRecords(block, ethLog)
        Assertions.assertThat(records).hasSize(eventData.size)

        records.forEachIndexed() { i, record ->
            Assertions.assertThat((record as ReversedEthereumLogRecord).data).isEqualTo(eventData[i])
        }
    }
}
