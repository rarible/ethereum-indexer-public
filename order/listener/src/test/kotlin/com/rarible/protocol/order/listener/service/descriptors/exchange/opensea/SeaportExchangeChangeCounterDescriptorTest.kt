package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.time.Instant

internal class SeaportExchangeChangeCounterDescriptorTest {

    private val contractsProvider = mockk<ContractsProvider>() {
        every { seaportV1() } returns listOf(randomAddress())
    }
    private val metrics: ForeignOrderMetrics = mockk {
        every { onOrderEventHandled(Platform.OPEN_SEA, "counter") } returns Unit
    }
    private val descriptor = SeaportExchangeChangeCounterDescriptor(
        contractsProvider,
        metrics
    )

    @Test
    fun `should convert SeaportExchangeChangeCounterDescriptor to event`() = runBlocking<Unit> {
        val transaction = mockk<Transaction>()
        val date = Instant.ofEpochSecond(1)
        val log = log(
            topics = listOf(
                Word.apply("0x721c20121297512b72821b97f5326877ea8ecf4bb9948fea5bfcb6453074d37f"),
                Word.apply("0x00000000000000000000000050bd1d1d160928a1c5923646a8474036e3c91c7d")
            ),
            data = "0x0000000000000000000000000000000000000000000000000000000000000001"
        )
        val ethBlock = EthereumBlockchainBlock(
            number = 1,
            hash = randomWord(),
            parentHash = randomWord(),
            timestamp = date.epochSecond,
            ethBlock = mockk()
        )
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = 1,
            total = 1,
        )
        val events = descriptor
            .getEthereumEventRecords(ethBlock, ethLog)
            .map { it.asEthereumLogRecord().data as ChangeNonceHistory }

        assertThat(events).hasSize(1)
        assertThat(events.single().date).isEqualTo(date)
        assertThat(events.single().newNonce).isEqualTo(EthUInt256.ONE)
        assertThat(events.single().maker).isEqualTo(Address.apply("0x50bd1d1d160928a1c5923646a8474036e3c91c7d"))

        verify(exactly = 1) { metrics.onOrderEventHandled(Platform.OPEN_SEA, "counter") }
    }
}