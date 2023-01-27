package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.contracts.blur.v1.evemts.OrdersMatchedEvent
import com.rarible.protocol.order.core.data.log
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.blur.BlurEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.response.Transaction
import java.time.Instant

class BlurV1ExchangeDescriptorTest {
    private val contractsProvider = mockk<ContractsProvider> {
        every { blurV1() } returns listOf(randomAddress())
    }
    private val blurEventConverter = mockk<BlurEventConverter>()
    private val subscriber = BlurV1ExchangeDescriptor(contractsProvider, blurEventConverter)

    @Test
    fun `convert cancel`() = runBlocking<Unit> {
        val index = 1
        val totalLogs = 10
        val date = Instant.ofEpochSecond(1000)

        val log = log(listOf(Word.apply(randomWord())))
        val transaction = mockk<Transaction> {
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.empty()
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
        val match1 = mockk<OrderSideMatch>()
        val match2 = mockk<OrderSideMatch>()

        coEvery { blurEventConverter.convert(log, date, transaction.input()) } returns listOf(match1, match2)

        val cancels = subscriber.getEthereumEventRecords(block, ethLog)
        assertThat(cancels).hasSize(2)
        assertThat((cancels[0] as ReversedEthereumLogRecord).data).isEqualTo(match1)
        assertThat((cancels[1] as ReversedEthereumLogRecord).data).isEqualTo(match2)
    }
}