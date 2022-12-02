package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.core.service.ContractsProvider
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.time.Instant

internal class SeaportExchangeChangeCounterDescriptorTest {
    private val contractsProvider = mockk<ContractsProvider>() {
        every { seaportV1() } returns listOf(randomAddress())
    }
    private val seaportCounterEventCounter = mockk<RegisteredCounter> {
        every { increment() } returns Unit
    }
    private val descriptor =  SeaportExchangeChangeCounterDescriptor(
        contractsProvider,
        seaportCounterEventCounter
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
        val events = descriptor.convert(log, transaction, date.epochSecond, 1, 1).toFlux().collectList().awaitSingle()
        assertThat(events).hasSize(1)
        assertThat(events.single().date).isEqualTo(date)
        assertThat(events.single().newNonce).isEqualTo(EthUInt256.ONE)
        assertThat(events.single().maker).isEqualTo(Address.apply("0x50bd1d1d160928a1c5923646a8474036e3c91c7d"))

        verify(exactly = 1) { seaportCounterEventCounter.increment() }
    }
}