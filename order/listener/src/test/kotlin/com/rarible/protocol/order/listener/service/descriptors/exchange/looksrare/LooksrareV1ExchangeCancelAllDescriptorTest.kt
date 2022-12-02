package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.core.service.ContractsProvider
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class LooksrareV1ExchangeCancelAllDescriptorTest {
    private val looksrareCancelAllEventMetric = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val contractsProvider = mockk<ContractsProvider> {
        every { looksrareV1() } returns listOf(randomAddress())
    }
    private val descriptor = LooksrareV1ExchangeCancelAllDescriptor(
        contractsProvider,
        looksrareCancelAllEventMetric
    )

    @Test
    fun `should convert cancelAll event`() = runBlocking<Unit> {
        val transaction = mockk<Transaction>()
        val data = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0x1e7178d84f0b0825c65795cd62e7972809ad3aac6917843aaec596161b2c0a97"),
                Word.apply("0x00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6")
            ),
            "0000000000000000000000000000000000000000000000000000000000000003"
        )
        val cancels = descriptor.convert(log, transaction, data.epochSecond, 0, 0).toFlux().collectList().awaitFirst()
        Assertions.assertThat(cancels).hasSize(1)
        Assertions.assertThat(cancels.single().maker).isEqualTo(Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"))
        Assertions.assertThat(cancels.single().newNonce).isEqualTo(EthUInt256.of(3))
        Assertions.assertThat(cancels.single().date).isEqualTo(data)
        Assertions.assertThat(cancels.single().source).isEqualTo(HistorySource.LOOKSRARE)
    }
}